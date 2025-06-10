package com.example.datalake.ingestionsvc.service;

import com.example.datalake.ingestionsvc.dto.FileIngestionRequest;
import com.example.datalake.ingestionsvc.events.IngestionCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Robust schema‑aware CSV → Postgres loader.
 * • Converts values to native types before binding.
 * • Skips rows missing a NOT‑NULL, no‑default column.
 * • Parses timestamps with or without offset.
 * Kafka publishing remains disabled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchIngestionService {

    @Qualifier("ingestionExecutor")
    private final TaskExecutor ingestionExecutor;
    private final StorageClientService storage;
    private final JdbcTemplate jdbc;          // for schema lookups
    private final DataSource dataSource;      // for high‑throughput inserts

    private static final int BATCH_SIZE  = 1_000;
    private static final Duration SCHEMA_TTL = Duration.ofHours(1);

    private record ColumnMeta(String name, int sqlType, boolean nullable, boolean hasDefault){}
    private record Cached(List<ColumnMeta> cols, Instant loadedAt){}
    private final ConcurrentMap<String, Cached> cache = new ConcurrentHashMap<>();

    public void enqueue(FileIngestionRequest req) {
        log.info("Enqueue ingestion: table='{}', url='{}'", req.tableName(), req.url());
        ingestionExecutor.execute(() -> runJob(req));
    }

    @Transactional
    void runJob(FileIngestionRequest req) {
        long ok = 0, fail = 0;
        String table = req.tableName().toLowerCase(Locale.ROOT);
        try (var in = storage.downloadStream(req.url());
             var csv = new CSVReaderBuilder(new InputStreamReader(in))
                     .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                     .build()) {

            String[] headerRaw = csv.readNext();
            if (headerRaw == null) throw new IllegalStateException("Empty CSV");
            String[] header = Arrays.stream(headerRaw)
                    .map(h -> h.toLowerCase(Locale.ROOT).trim())
                    .toArray(String[]::new);

            List<ColumnMeta> schema = getSchema(table);
            Set<String> csvCols = Set.of(header);
            List<ColumnMeta> cols = schema.stream()
                    .filter(c -> csvCols.contains(c.name()) || (!c.hasDefault() && !c.nullable()))
                    .toList();
            if (cols.isEmpty()) throw new IllegalStateException("No matching columns for table " + table);
            String sql = buildInsertSql(table, cols);

            List<Map<String, String>> batch = new ArrayList<>(BATCH_SIZE);
            String[] line;
            long lineNo = 1;
            while ((line = csv.readNext()) != null) {
                lineNo++;
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < header.length && i < line.length; i++) {
                    row.put(header[i], line[i]);
                }
                batch.add(row);
                if (batch.size() == BATCH_SIZE) {
                    long[] stats = flushBatch(sql, cols, batch);
                    ok += stats[0]; fail += stats[1];
                    log.info("Batch up to line {} -> {} ok / {} fail", lineNo, stats[0], stats[1]);
                }
            }
            if (!batch.isEmpty()) {
                long[] stats = flushBatch(sql, cols, batch);
                ok += stats[0]; fail += stats[1];
            }
        } catch (Exception e) {
            log.error("Fatal ingestion error for table '{}': {}", table, e.getMessage(), e);
            ok = 0; fail = -1;
        }
        log.info("Ingestion done for table='{}': success={}, fail={}", table, ok, fail);
    }

    private List<ColumnMeta> getSchema(String table) {
        return cache.compute(table, (k, old) ->
                (old == null || Instant.now().isAfter(old.loadedAt().plus(SCHEMA_TTL)))
                        ? new Cached(loadSchema(k), Instant.now())
                        : old
        ).cols();
    }

    private List<ColumnMeta> loadSchema(String table) {
        return jdbc.query(
                """
                SELECT column_name,
                       data_type,
                       is_nullable = 'YES'        AS nullable,
                       column_default IS NOT NULL AS has_default
                FROM information_schema.columns
                WHERE table_name = ?
                ORDER BY ordinal_position
                """,
                (rs, idx) -> new ColumnMeta(
                        rs.getString("column_name").toLowerCase(Locale.ROOT),
                        mapSqlType(rs.getString("data_type")),
                        rs.getBoolean("nullable"),
                        rs.getBoolean("has_default")
                ),
                table
        );
    }

    private static int mapSqlType(String dataType) {
        return switch (dataType) {
            case "integer" -> Types.INTEGER;
            case "bigint" -> Types.BIGINT;
            case "numeric", "decimal" -> Types.NUMERIC;
            case "double precision" -> Types.DOUBLE;
            case "boolean" -> Types.BOOLEAN;
            case "timestamp without time zone", "timestamp with time zone" -> Types.TIMESTAMP;
            default -> Types.VARCHAR;
        };
    }

    private String buildInsertSql(String table, List<ColumnMeta> cols) {
        String colNames = cols.stream()
                .map(ColumnMeta::name)
                .collect(Collectors.joining(","));
        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        return "INSERT INTO " + table + " (" + colNames + ") VALUES (" + placeholders + ")";
    }

    private long[] flushBatch(String sql, List<ColumnMeta> cols, List<Map<String, String>> batch) {
        int totalRows = batch.size();
        int invalidRows = 0;
        int validRows = 0;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (Map<String, String> row : batch) {
                boolean rowValid = true;
                int idx = 1;
                for (ColumnMeta c : cols) {
                    String raw = row.get(c.name());
                    try {
                        if (raw == null || raw.isBlank()) {
                            ps.setNull(idx++, c.sqlType());
                        } else {
                            switch (c.sqlType()) {
                                case Types.INTEGER -> ps.setInt(idx++, Integer.parseInt(raw));
                                case Types.BIGINT  -> ps.setLong(idx++, Long.parseLong(raw));
                                case Types.DOUBLE, Types.NUMERIC -> ps.setDouble(idx++, Double.parseDouble(raw));
                                case Types.BOOLEAN -> ps.setBoolean(idx++, Boolean.parseBoolean(raw));
                                case Types.TIMESTAMP -> ps.setTimestamp(idx++, parseTimestamp(raw));
                                default -> ps.setString(idx++, raw);
                            }
                        }
                    } catch (Exception e) {
                        rowValid = false;
                        break;
                    }
                }
                if (rowValid) {
                    ps.addBatch();
                    validRows++;
                } else {
                    invalidRows++;
                }
            }
            int[] results = ps.executeBatch();
            long success = Arrays.stream(results).filter(count -> count > 0).count();
            long fail = (validRows - success) + invalidRows;
            return new long[]{success, fail};
        } catch (SQLException | DataAccessException e) {
            log.warn("DB insert error on {} rows: {}", totalRows, e.getMessage());
            return new long[]{0, totalRows};
        } finally {
            batch.clear();
        }
    }

    private static Timestamp parseTimestamp(String s) {
        s = s.trim();
        if (s.matches(".*[+-]\\d{1,2}$")) {
            s = s + ":00";
        }
        String iso = s.contains("T") ? s : s.replace(' ', 'T');
        try {
            OffsetDateTime odt = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Timestamp.from(odt.toInstant());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Timestamp.valueOf(s.replace('T', ' '));
        } catch (IllegalArgumentException ignored) {
        }
        throw new IllegalArgumentException("Unparsable timestamp: " + s);
    }
}

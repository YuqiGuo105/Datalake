package com.example.datalake.ingestionsvc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Produces a per-table {@link TableValidator} that converts raw CSV rows into
 * typed values.  Schemas are cached and refreshed only after a TTL expires.
 */
@Service
@RequiredArgsConstructor
public class ValidatorService {

    private static final Duration SCHEMA_TTL = Duration.ofHours(1);

    private final JdbcTemplate jdbc;

    private record CachedSchema(Map<String,Integer> cols, Instant loadedAt) { }
    private final ConcurrentMap<String, CachedSchema> cache = new ConcurrentHashMap<>();

    /* -------- create / refresh schema cache -------- */

    public TableValidator forTable(String table) {
        CachedSchema cached = cache.compute(table.toLowerCase(), (t, old) ->
                (old == null || expired(old.loadedAt))
                        ? new CachedSchema(loadSchema(t), Instant.now())
                        : old);
        return new TableValidator(cached.cols);
    }

    private boolean expired(Instant t) { return Instant.now().isAfter(t.plus(SCHEMA_TTL)); }

    /* ---------------- inner helper ---------------- */

    public static final class TableValidator {
        private final Map<String,Integer> schema;
        private final Set<String> timestampCols;

        private TableValidator(Map<String,Integer> schema) {
            this.schema = schema;
            /* pre-compute the set of timestamp columns so the per-row loop is fast */
            this.timestampCols = schema.entrySet().stream()
                    .filter(e -> e.getValue() == Types.TIMESTAMP)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toUnmodifiableSet());
        }

        /** Convert one raw CSV row → typed map; skip TIMESTAMP columns. */
        public Map<String,Object> convert(Map<String,String> raw) {
            Map<String,Object> out = new LinkedHashMap<>();
            for (var e : raw.entrySet()) {
                String col = e.getKey().toLowerCase();
                if (timestampCols.contains(col)) continue;          // ← skip timestamps
                Integer sqlType = schema.get(col);
                if (sqlType == null) continue;                      // skip unknown
                out.put(col, cast(e.getValue(), sqlType));
            }
            return out;
        }

        /* ------- primitive casting (unchanged except no TIMESTAMP case) ------- */

        private static Object cast(String v, int sqlType) {
            if (v == null || v.isBlank()) return null;
            return switch (sqlType) {
                case Types.INTEGER, Types.SMALLINT  -> Integer.parseInt(v);
                case Types.BIGINT                   -> Long.parseLong(v);
                case Types.NUMERIC, Types.DECIMAL   -> new java.math.BigDecimal(v);
                case Types.BOOLEAN                  -> Boolean.parseBoolean(v);
                default                             -> v;           // VARCHAR, TEXT, …
            };
        }
    }

    /* ----------- load schema once from information_schema ----------- */

    private Map<String,Integer> loadSchema(String table) {
        return jdbc.query("""
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_name = ?
                """,
                rs -> {
                    Map<String,Integer> m = new HashMap<>();
                    while (rs.next()) {
                        m.put(rs.getString("column_name").toLowerCase(),
                                sqlType(rs.getString("data_type")));
                    }
                    return m;
                }, table);
    }

    private static int sqlType(String dataType) {
        return switch (dataType) {
            case "integer"  -> Types.INTEGER;
            case "bigint"   -> Types.BIGINT;
            case "numeric", "decimal" -> Types.NUMERIC;
            case "timestamp without time zone",
                    "timestamp with time zone"        -> Types.TIMESTAMP;
            case "boolean"  -> Types.BOOLEAN;
            default         -> Types.VARCHAR;
        };
    }
}

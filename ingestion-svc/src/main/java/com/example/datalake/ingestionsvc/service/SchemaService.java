package com.example.datalake.ingestionsvc.service;

import com.example.datalake.ingestionsvc.dto.CreateTableRequest;
import com.example.datalake.ingestionsvc.events.TableCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

/** Core logic for CREATE TABLE & Kafka notify */
@Service
@RequiredArgsConstructor
public class SchemaService {
    private final JdbcTemplate jdbc;                 // from DatabaseConfig
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @Value("${topic.metadata.table}")
    private String topic;

    /** High-level API called by controller */
    @Transactional
    public void createTable(CreateTableRequest req) {
        jdbc.execute(buildCreateTableSql(req));      // Table

        req.columns().stream()                       // Indexes
                .filter(CreateTableRequest.ColumnSpec::indexed)
                .forEach(c -> createIndex(req.tableName(), c.name()));

//        publishEvent(req);                           // Event
    }

    /* ---------- helpers ---------- */
    private String buildCreateTableSql(CreateTableRequest req) {
        String cols = req.columns().stream()
                .map(this::columnDef).collect(Collectors.joining(", "));

        String pks = req.columns().stream()
                .filter(CreateTableRequest.ColumnSpec::primaryKey)
                .map(CreateTableRequest.ColumnSpec::name)
                .collect(Collectors.joining(", "));

        return "CREATE TABLE IF NOT EXISTS " + q(req.tableName()) +
                " (" + cols +
                (pks.isBlank() ? "" : ", PRIMARY KEY (" + pks + ")") +
                ");";
    }

    private String columnDef(CreateTableRequest.ColumnSpec c) {
        return q(c.name()) + " " + c.type().name() +
                (c.nullable() ? "" : " NOT NULL");
    }

    private void createIndex(String table, String column) {
        String idx = table + "_" + column + "_idx";
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + q(idx) +
                " ON " + q(table) + " (" + q(column) + ");");
    }

    private void publishEvent(CreateTableRequest req) {
        var evt = new TableCreatedEvent(
                req.tableName(),
                req.creator(),
                req.columns().stream()
                        .map(c -> new TableCreatedEvent.ColumnMeta(
                                c.name(), c.type().name(),
                                c.primaryKey(), c.indexed()))
                        .toList(),
                Instant.now()
        );
        try {
            kafka.send(topic, mapper.writeValueAsString(evt));
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish schema event", e);
        }
    }

    private static String q(String id) { return "\"" + id.replace("\"","\"\"") + "\""; }
}

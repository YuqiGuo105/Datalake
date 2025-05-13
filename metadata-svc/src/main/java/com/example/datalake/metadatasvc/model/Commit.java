package com.example.datalake.metadatasvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dataset_commits")
@Getter
@Setter
public class Commit {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();
    @Column(name = "dataset_id", length = 36, nullable = false)
    private String datasetId;
    @Column(nullable = false)
    private Integer version;
    @Column(name = "commit_time", nullable = false)
    private Instant commitTime = Instant.now();
    private String author;
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> extraMeta;
}

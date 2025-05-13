package com.example.datalake.metadatasvc.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "datasets")
@Getter
@Setter
public class DataSet {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();
    @Column(length = 128, nullable = false, unique = true)
    private String name;
    @Column(name = "storage_uri", length = 1024, nullable = false)
    private String storageUri;
    @Column(length = 16, nullable = false)
    private String format;
    @Column(length = 64)
    private String project;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "created_by", length = 64)
    private String createdBy;
    @Column(name = "last_commit_id", length = 36)
    private String lastCommitId;
    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Commit> commits = new HashSet<>();
    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DataSetColumn> columns = new HashSet<>();
}

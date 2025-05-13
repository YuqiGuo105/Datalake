package com.example.datalake.metadatasvc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Entity
@Table(name = "dataset_columns")
@Getter
@Setter
public class DataSetColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private DataSet dataset;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "column_name", length = 128, nullable = false)
    private String columnName;

    @Column(name = "data_type", length = 64, nullable = false)
    private String dataType;

    @Column(name = "is_partition", nullable = false)
    private Boolean isPartition = false;

    @Column(name = "is_primary_key", nullable = false)
    private Boolean isPrimaryKey = false;

    @Column(length = 256)
    private String comment;
}

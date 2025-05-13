package com.example.datalake.metadatasvc.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DataSetDTO {
    private String id;
    private String name;
    private String storageUri;
    private String format;
    private String project;
    private Instant createdAt;
    private String createdBy;
    private String lastCommitId;
}

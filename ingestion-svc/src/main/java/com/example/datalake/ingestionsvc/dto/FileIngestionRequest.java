package com.example.datalake.ingestionsvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "FileIngestionRequest",
        description = "Batch-ingestion request: identifies storage object & target table.")
public record FileIngestionRequest(
        @Schema(description = "Object key (path) inside bucket",
                example = "uploads/2025/06/sales.csv")
        @NotBlank String url,

        @Schema(description = "Target Postgres table for inserting parsed rows",
                example = "sales")
        @NotBlank String tableName,

        @Schema(description = "Uploader e-mail / userId for auditing",
                example = "alice@example.com")
        @NotBlank String uploader
) { }


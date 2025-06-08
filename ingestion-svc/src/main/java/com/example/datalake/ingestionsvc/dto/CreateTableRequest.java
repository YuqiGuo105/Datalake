package com.example.datalake.ingestionsvc.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(
        name        = "CreateTableRequest",
        description = "Payload to define a new table schema"
)
public record CreateTableRequest(
        @NotBlank
        @Schema(description = "Identifier for the new table", example = "orders")
        String tableName,

        @NotEmpty
        @Size(min = 1)
        @Schema(
                description = "Non‐empty list of column definitions",
                implementation = CreateTableRequest.ColumnSpec.class
        )
        List<ColumnSpec> columns,

        @NotBlank
        @Schema(description = "User or system creating this schema", example = "yuqi.guo@example.com")
        String creator
) {
    @Schema(
            name        = "ColumnSpec",
            description = "Specification for a single column"
    )
    public record ColumnSpec(
            @NotBlank
            @Schema(description = "Column name (unique within table)", example = "order_id")
            String name,

            @NotNull
            @Schema(
                    description = "SQL data type; must be one of the enum values",
                    example     = "BIGINT"
            )
            SqlType type,

            @Schema(description = "Whether NULL values are allowed", example = "false")
            boolean nullable,

            @Schema(description = "Whether this column is the primary key", example = "true")
            boolean primaryKey,

            @Schema(description = "Whether to create an index on this column", example = "true")
            boolean indexed
    ) { }

    @Schema(description = "Supported SQL data types")
    public enum SqlType {
        TEXT,
        VARCHAR,
        INT,
        BIGINT,
        NUMERIC,
        TIMESTAMP,
        DATE,
        BOOLEAN
    }
}

package com.example.datalake.metadatasvc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ColumnDTO {
    @NotBlank
    private String columnName;

    @NotBlank
    private String dataType;

    private Boolean isPartition = false;

    private Boolean isPrimaryKey = false;

    private String comment;
}

package com.example.datalake.metadatasvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class NewDataSetDTO {
    @NotBlank(message = "name cannot be blank")
    private String name;

    String description;

    @NotBlank(message = "storageUri cannot be blank")
    private String storageUri;

    @NotBlank(message = "format cannot be blank")
    @Pattern(regexp = "ICEBERG|DELTA|HUDI|PARQUET",
            message = "format must be one of ICEBERG / DELTA / HUDI / PARQUET")
    private String format;

    @NotBlank(message = "Owner User ID cannot be blank")
    private String ownerUserId;

    @NotBlank(message = "Owner Group cannot be blank")
    private String ownerGroupId;

    private String project;
}

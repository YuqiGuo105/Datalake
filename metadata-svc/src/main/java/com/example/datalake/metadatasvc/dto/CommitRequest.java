package com.example.datalake.metadatasvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CommitRequest {
    @NotBlank
    private String message;

    @NotEmpty
    private List<ColumnDTO> columns;
}

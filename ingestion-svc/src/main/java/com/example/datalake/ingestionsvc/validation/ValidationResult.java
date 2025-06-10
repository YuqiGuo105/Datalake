package com.example.datalake.ingestionsvc.validation;

import java.util.List;
import java.util.Map;

public record ValidationResult(
        List<Map<String,Object>> validRows,
        List<FailedRow> failedRows
) {
    public record FailedRow(int lineNumber, String reason) { }
}

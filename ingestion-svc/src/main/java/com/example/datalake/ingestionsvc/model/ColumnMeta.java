package com.example.datalake.ingestionsvc.model;

public record ColumnMeta(
        String name,         // lowercase
        int    sqlType,      // java.sql.Types
        boolean nullable,
        boolean hasDefault
) {
}

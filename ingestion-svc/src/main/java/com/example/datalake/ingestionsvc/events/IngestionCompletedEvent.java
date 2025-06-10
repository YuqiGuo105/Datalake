package com.example.datalake.ingestionsvc.events;

import java.time.Instant;

public record IngestionCompletedEvent(
        String tableName,
        String filePath,
        String uploader,
        long successRows,
        long failedRows,
        Instant finishedAt
) {
}

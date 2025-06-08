package com.example.datalake.ingestionsvc.events;

import java.time.Instant;
import java.util.List;

public record TableCreatedEvent( String tableName,
                                 String creator,
                                 List<ColumnMeta> columns,
                                 Instant createdAt) {
    public record ColumnMeta(
            String name,
            String type,
            boolean pk,
            boolean idx
    ) { }
}

package com.example.datalake.ingestionsvc.dto;

import java.time.Instant;

public record FileMetadataRequest(String datasetName,
                                  String path,
                                  long   sizeBytes,
                                  Instant ingestedAt) {
}

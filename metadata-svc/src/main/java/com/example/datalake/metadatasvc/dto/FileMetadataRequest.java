package com.example.datalake.metadatasvc.dto;

import java.time.Instant;

public record FileMetadataRequest( String fileName,
                                   String path,
                                   long   sizeBytes,
                                   Instant ingestedAt,
                                   String userId,     // NEW – who uploaded
                                   String groupId     // NEW – which team / project
                                   ) {
}

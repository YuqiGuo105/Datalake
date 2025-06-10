package com.example.datalake.ingestionsvc.controller;

import com.example.datalake.ingestionsvc.dto.FileIngestionRequest;
import com.example.datalake.ingestionsvc.service.BatchIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingest/files")
@Tag(name = "Batch Ingestion", description = "Upload & ingest large CSV files")
@RequiredArgsConstructor
public class BatchIngestionController {
    private final BatchIngestionService service;

    /** POST /ingest/files  → 202 Accepted + async processing */
    @Operation(
            summary     = "Batch-ingest a CSV file",
            description = """
                     Given the <bucket, path> of a file in Supabase Storage, this endpoint will:
                                                                 1) asynchronously download the file,
                                                                 2) validate its contents,
                                                                 3) batch-insert the records into the database.
                                                                \s
                                                                 On success, returns HTTP 202 (Accepted). The final row count will be
                                                                 reported later to metadata-svc via Kafka.
                      """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description   = "Accepted – ingestion job queued"),
            @ApiResponse(responseCode = "400",
                    description   = "Validation error",
                    content       = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description   = "Server error",
                    content       = @Content)
    })
    @PostMapping
    public ResponseEntity<Void> ingest(@Valid @RequestBody FileIngestionRequest req) {
        service.enqueue(req);
        return ResponseEntity.accepted().build();
    }
}

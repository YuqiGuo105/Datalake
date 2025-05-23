package com.example.datalake.ingestionsvc.controller;

import com.example.datalake.ingestionsvc.dto.FileMetadataRequest;
import com.example.datalake.ingestionsvc.dto.PresignedUrlResponse;
import com.example.datalake.ingestionsvc.event.IngestionEventPublisher;
import com.example.datalake.ingestionsvc.feign.MetadataClient;
import com.example.datalake.ingestionsvc.security.JwtUtil;
import com.example.datalake.ingestionsvc.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/ingestion")
@RequiredArgsConstructor
public class IngestionController {
    private final JwtUtil jwt;
    private final FileStorageService storage;
    private final IngestionEventPublisher publisher;
    private final MetadataClient datasetClient;
    /**
     * Generate a one-time upload URL
     * Returns a local upload endpoint with a time-limited JWT
     */
    @Operation(
            summary = "Request a presigned upload URL",
            description = "Backend returns a local upload path containing a one-time JWT"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned URL generated successfully")
    })
    @GetMapping("/presign")
    public PresignedUrlResponse presign(@RequestParam("filename") String filename) {
        String token = jwt.issue(filename, Duration.ofHours(1));
        return new PresignedUrlResponse("/ingestion/upload/" + token,
                Instant.now().plus(Duration.ofHours(1)));
    }

    /**
     * Upload a file via Multipart form
     * Client uploads the file under 'file' part field
     */
    @Operation(
            summary = "Upload a file to a dataset",
            description = "Uploads a file using a token and registers it in an existing dataset. Also publishes a Kafka event for asynchronous processing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid token", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/{token}")
    public Mono<ResponseEntity<Void>> upload(
            @Parameter(description = "JWT token with embedded filename", required = true)
            @PathVariable String token,

            @Parameter(description = "ID of the dataset", required = true)
            @RequestParam String datasetId,

            @Parameter(description = "ID of the user performing the upload", required = true)
            @RequestHeader("X-User-Id") String userId,

            @Parameter(description = "ID of the group the user belongs to", required = true)
            @RequestHeader("X-Group-Id") String groupId,

            @Parameter(description = "The file to upload", required = true, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") FilePart file
    ) throws Exception {

        String fileName = jwt.verify(token).get("filename", String.class);
        return storage.save(file, fileName)                    // Mono<Path>
                .flatMap(path ->
                        Mono.fromCallable(() -> {              // blocking section
                                    datasetClient.registerIngestion(
                                            datasetId,
                                            new FileMetadataRequest(
                                                    fileName,
                                                    path.toString(),
                                                    Files.size(path),
                                                    Instant.now(),
                                                    userId,
                                                    groupId));
                                    return path;
                                })
                                .subscribeOn(Schedulers.boundedElastic()))
                .doOnSuccess(path -> publisher.publish(fileName, path))
                .thenReturn(ResponseEntity.ok().build());
    }

    /**
     * Streamed real-time ingestion (ND-JSON / CSV lines)
     * POST a plain text stream; backend will append each line to file
     */
    @Operation(
            summary = "Stream real-time data",
            description = "Accepts line-delimited plain text via POST and appends it to a file"
    )
    @PostMapping(path = "/stream/{token}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<Void>> stream(
            @PathVariable String token,
            @RequestBody Flux<String> lines) {

        String filename = jwt.verify(token).get("filename", String.class);
        return storage.saveStream(lines, filename)
                .doOnSuccess(path -> publisher.publish(filename, path))
                .thenReturn(ResponseEntity.accepted().build());
    }
}

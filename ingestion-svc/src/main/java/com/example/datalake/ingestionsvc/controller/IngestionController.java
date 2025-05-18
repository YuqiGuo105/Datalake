package com.example.datalake.ingestionsvc.controller;

import com.example.datalake.ingestionsvc.dto.PresignedUrlResponse;
import com.example.datalake.ingestionsvc.event.IngestionEventPublisher;
import com.example.datalake.ingestionsvc.security.JwtUtil;
import com.example.datalake.ingestionsvc.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.multipart.FilePart;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/ingestion")
@RequiredArgsConstructor
public class IngestionController {
    private final JwtUtil jwt;
    private final FileStorageService storage;
    private final IngestionEventPublisher publisher;

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
    public PresignedUrlResponse presign(@RequestParam String filename) {
        String token = jwt.issue(filename, Duration.ofHours(1));
        return new PresignedUrlResponse("/ingestion/upload/" + token,
                Instant.now().plus(Duration.ofHours(1)));
    }

    /**
     * Upload a file via Multipart form
     * Client uploads the file under 'file' part field
     */
    @Operation(
            summary = "Upload a file",
            description = "Client uploads a file to local disk using the 'file' form field"
    )
    @PostMapping(path = "/upload/{token}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Void>> upload(
            @PathVariable String token,
            @RequestPart("file") FilePart file) {
        String filename = jwt.verify(token).get("filename", String.class);
        return storage.save(file, filename)
                .doOnSuccess(path -> publisher.publish(filename, path))
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

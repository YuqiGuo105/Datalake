package com.example.datalake.ingestionsvc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Stream large objects from Supabase Storage without loading into memory */
@Service
@RequiredArgsConstructor
public class StorageClientService {

    private final WebClient supabaseWebClient;   // from SupabaseConfig

    /**
     * Stream an object from Supabase Storage into a temp file and return FileInputStream.
     * The temp file auto-deletes when stream is closed (DELETE_ON_CLOSE).
     */
    public InputStream downloadStream(String fileUrl) {
        try {
            Path tmp = Files.createTempFile("supabase-", ".tmp");
            tmp.toFile().deleteOnExit();     // JVM-level safety net

            // Non-blocking download → write to temp file
            Flux<DataBuffer> body = supabaseWebClient       // reuse the same WebClient
                    .get()
                    .uri(fileUrl)                           // <-- use full URL
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            DataBufferUtils.write(body, tmp, StandardOpenOption.WRITE)
                    .block();                        // wait for completion

            // InputStream that deletes the temp file when closed
            return Files.newInputStream(tmp, StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException ioe) {
            throw new UncheckedIOException("Temp-file creation failed", ioe);
        } catch (Exception ex) {
            throw new RuntimeException("Supabase download failed", ex);
        }
    }
}

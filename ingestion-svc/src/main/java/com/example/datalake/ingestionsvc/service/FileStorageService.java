package com.example.datalake.ingestionsvc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${ingestion.storage.path}")
    private Path rootDir;

    /** Save Multipart file (for batch upload scenarios) */
    public Mono<Path> save(FilePart part, String filename) {
        Path dest = rootDir.resolve(filename).normalize();
        return part.transferTo(dest).thenReturn(dest);
    }

    /** Save line-based streaming data such as ND-JSON/CSV */
    public Mono<Path> saveStream(Flux<String> lines, String filename) {
        Path dest = rootDir.resolve(filename).normalize();
        DataBufferFactory f = new DefaultDataBufferFactory();
        return DataBufferUtils.write(
                        lines.map(s -> f.wrap((s + System.lineSeparator())
                                .getBytes(StandardCharsets.UTF_8))),
                        dest,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)
                .thenReturn(dest);
    }
}

package com.example.datalake.ingestionsvc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class StorageService {
    private final WebClient webClient;
    private final String defaultBucket;

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public StorageService(WebClient supabaseWebClient,
                          @Value("${supabase.bucket}") String defaultBucket) {
        this.webClient = supabaseWebClient;
        this.defaultBucket = defaultBucket; // e.g. "test"
    }

    /**
     * 1) List all buckets in your project
     *    GET /bucket
     */
    public Mono<String> listBuckets() {
        return webClient.get()
                .uri("/bucket")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "Failed to list all buckets", e)));
    }

    /**
     * 2) Create a new bucket
     *    POST /bucket   body: { "name": "<bucketName>", "public": true/false }
     */
    public Mono<String> createBucket(String bucketName, boolean isPublic) {
        String bodyJson = String.format("{\"name\":\"%s\",\"public\":%b}", bucketName, isPublic);
        return webClient.post()
                .uri("/bucket")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyJson)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "Failed to create bucket", e)));
    }

    /**
     * 3) List objects in a specific bucket
     *    GET /object/list/{bucketId}?limit=100&offset=0
     */
    public Mono<String> listObjects(String bucket) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/object/list/{bucketId}")
                        .queryParam("limit", 100)
                        .queryParam("offset", 0)
                        .build(bucket))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "Failed to list objects", e)));
    }

    /**
     * 4) Upload a file to a bucket (e.g. from a Spring MultipartFile/FilePart)
     *    POST /object/{bucketId}
     *    form‐data: "file" → binary, "path" → target path inside bucket
     */
    public Mono<String> uploadFile(String bucket,
                                   String objectPath,
                                   FilePart filePart) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/object/{bucketId}")
                        .build(bucket))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", filePart)
                        .with("path", objectPath))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "File upload failed", e)));
    }

    /**
     * 5) Download a file from a public bucket:
     *    GET /object/public/{bucketId}/{objectPath}
     *    returns raw bytes as ByteArrayResource
     */
    public Mono<ByteArrayResource> downloadFile(String bucket, String objectPath) {
        return webClient.get()
                .uri("/object/public/{bucketId}/{objectPath}", bucket, objectPath)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(ByteArrayResource.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "Download failed", e)));
    }

    /**
     * 6) Delete an object:
     *    DELETE /object/{bucketId}/{objectPath}
     */
    public Mono<String> deleteObject(String bucket, String objectPath) {
        return webClient.delete()
                .uri("/object/{bucketId}/{objectPath}", bucket, objectPath)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "Failed to delete object", e)));
    }

    /**
     * Upload raw bytes directly to Supabase Storage under defaultBucket.
     *
     * @param objectPath  The path inside bucket (e.g. "avatars/user123.png")
     * @param data        The raw bytes of the file
     * @param contentType Must match the file mime (e.g. "image/png")
     */
    public Mono<String> uploadBinary(String objectPath, byte[] data, String contentType) {
        return webClient.post()
                // maps to POST /object/{defaultBucket}/{objectPath}
                .uri("/object/{bucket}/{path}", defaultBucket, objectPath)
                .header("Content-Type", contentType)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(
                        new ResponseStatusException(500, "Binary upload failed", e)));
    }
}

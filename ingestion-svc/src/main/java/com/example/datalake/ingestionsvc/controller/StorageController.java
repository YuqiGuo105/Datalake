package com.example.datalake.ingestionsvc.controller;

import com.example.datalake.ingestionsvc.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 1) List all buckets
     */
    @GetMapping("/buckets")
    public Mono<String> listBuckets() {
        return storageService.listBuckets();
    }

    /**
     * 2) Create a new bucket
     *    @param name     the name of the bucket to create
     *    @param isPublic whether the bucket will be publicly accessible
     */
    @PostMapping("/buckets")
    public Mono<String> createBucket(@RequestParam String name,
                                     @RequestParam(defaultValue = "false") boolean isPublic) {
        return storageService.createBucket(name, isPublic);
    }

    /**
     * 3) List all objects in the default bucket ("test")
     */
    @GetMapping("/objects")
    public Mono<String> listObjects() {
        return storageService.listObjects(storageService.getDefaultBucket());
    }

    /**
     * 4) Upload a file to the default bucket ("test")
     *    @param filePartMono a reactive FilePart containing the uploaded file
     *    @param path         the destination path inside the bucket
     *                        (e.g., "avatars/user123.png")
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadFile(@RequestPart("file") Mono<FilePart> filePartMono,
                                   @RequestParam String path) {
        return filePartMono
                .flatMap(fp -> storageService.uploadFile(storageService.getDefaultBucket(), path, fp));
    }

    /**
     * 5) Download a file from the default bucket (for public buckets)
     *    @param path the path of the file inside the bucket
     *                (e.g., "avatars/user123.png")
     */
    @GetMapping("/download")
    public Mono<ResponseEntity<ByteArrayResource>> downloadFile(@RequestParam String path) {
        return storageService.downloadFile(storageService.getDefaultBucket(), path)
                .map(resource -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + path + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource));
    }

    /**
     * 6) Delete a specific object from the default bucket
     *    @param path the path of the object to delete
     */
    @DeleteMapping("/delete")
    public Mono<String> deleteObject(@RequestParam String path) {
        return storageService.deleteObject(storageService.getDefaultBucket(), path);
    }

    /**
     * 7) Upload raw binary data to the default bucket
     *    Example:
     *      POST /api/storage/uploadBinary?path=avatars/cache.png
     *      Header: Content-Type: image/png (or application/octet-stream)
     *      Body  : raw binary payload
     */
    @PostMapping("/uploadBinary")
    public ResponseEntity<String> uploadBinary(
            @RequestParam("path") String path,
            HttpServletRequest request
    ) throws IOException {
        // 1) Read the Content-Type header (e.g. image/png)
        String contentType = request.getContentType();
        // 2) Read the full request body as a byte array
        byte[] data = StreamUtils.copyToByteArray(request.getInputStream());
        // 3) Forward payload to the reactive StorageService
        String resultJson = storageService
                .uploadBinary(path, data, contentType)
                .block();
        return ResponseEntity.ok(resultJson);
    }
}

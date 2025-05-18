package com.example.datalake.ingestionsvc.feign;

import com.example.datalake.ingestionsvc.dto.FileMetadataRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "metadata-svc", url = "${metadata.url}")
public interface MetadataClient {
    @PostMapping("/api/files")
    void register(@RequestBody FileMetadataRequest req);
}

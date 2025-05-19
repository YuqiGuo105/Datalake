package com.example.datalake.ingestionsvc.feign;

import com.example.datalake.ingestionsvc.dto.FileMetadataRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "metadata-svc", url = "${metadata.url}")
public interface MetadataClient {
    @PostMapping("/datasets/{id}/ingestions")
    void registerIngestion(@PathVariable("id") String datasetId,
                           @RequestBody FileMetadataRequest req);
}

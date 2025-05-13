package com.example.datalake.metadatasvc.controller;


import com.example.datalake.metadatasvc.dto.*;
import com.example.datalake.metadatasvc.service.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@Validated
public class DataSetController {
    private final MetaService metaService;

    @PostMapping
    public ResponseEntity<DataSetDTO> createDataSet(@Valid @RequestBody NewDataSetDTO body,
                                                    @RequestHeader(name = "X-User", defaultValue = "system") String user) {

        DataSetDTO dto = metaService.registerDataSet(body, user);

        // 201 Created + Location: /api/datasets/{id}
        return ResponseEntity
                .created(URI.create("/api/datasets/" + dto.getId()))
                .body(dto);
    }

    @PostMapping("/{id}/commits")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204，无响应体
    public void commit(@PathVariable String id,
                       @Valid @RequestBody CommitRequest body,
                       @RequestHeader(name = "X-User", defaultValue = "system") String user) {

        metaService.commitSchema(id, body.getColumns(), body.getMessage(), user);
    }

    @GetMapping("/{id}/schema")
    public List<ColumnDTO> getSchema(@PathVariable String id,
                                     @RequestParam(required = false) Integer version) {

        return metaService.getSchema(id, version);   // 返回 JSON 数组
    }

     @GetMapping
     public List<DataSetDTO> list() {
         return metaService.listAllDatasets();
     }
}

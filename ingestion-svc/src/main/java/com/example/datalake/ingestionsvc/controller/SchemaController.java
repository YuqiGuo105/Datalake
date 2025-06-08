package com.example.datalake.ingestionsvc.controller;

import com.example.datalake.ingestionsvc.dto.CreateTableRequest;
import com.example.datalake.ingestionsvc.service.SchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schemas")
@RequiredArgsConstructor
@Tag(name = "Schema", description = "Schema management endpoints")
public class SchemaController {
    private final SchemaService schemaService;

    @PostMapping
    @Operation(
            summary = "Create a new table schema",
            description = "Accepts a JSON payload describing table name, column specs, and creator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted — schema creation in progress"),
            @ApiResponse(responseCode = "400", description = "Bad Request — validation failed")
    })
    public ResponseEntity<Void> create(@Valid @RequestBody CreateTableRequest req) {
        schemaService.createTable(req);
        return ResponseEntity.accepted().build();   // 202
    }
}

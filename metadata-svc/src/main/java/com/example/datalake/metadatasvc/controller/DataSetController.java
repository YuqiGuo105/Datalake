package com.example.datalake.metadatasvc.controller;


import com.example.datalake.metadatasvc.dto.*;
import com.example.datalake.metadatasvc.service.MetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "Create a new dataset",
            description = "Registers a new dataset and returns the created dataset with its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dataset created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataSetDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<DataSetDTO> createDataSet(
            @Parameter(description = "New dataset information", required = true)
            @Valid @RequestBody NewDataSetDTO body,
            @Parameter(description = "Username performing the action", required = true)
            @RequestHeader(name = "X-User", defaultValue = "system") String user) {

        DataSetDTO dto = metaService.registerDataSet(body, user);

        // 201 Created + Location: /api/datasets/{id}
        return ResponseEntity
                .created(URI.create("/api/datasets/" + dto.getId()))
                .body(dto);
    }

    @Operation(
            summary = "Commit schema changes to a dataset",
            description = "Adds a new version of the schema to an existing dataset."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Commit successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Dataset not found")
    })
    @PostMapping("/{id}/commits")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204，No response
    public void commit(
            @Parameter(name = "id",
                    in = ParameterIn.PATH,
                    description = "Dataset ID",
                    required = true)
            @PathVariable("id") String id,
            @Parameter(description = "Commit details", required = true)
            @Valid @RequestBody CommitRequest body,
            @Parameter(description = "Username performing the action", required = true)
            @RequestHeader(name = "X-User", defaultValue = "system") String user) {

        metaService.commitSchema(id, body.getColumns(), body.getMessage(), user);
    }

    @Operation(
            summary = "Retrieve dataset schema",
            description = "Returns the schema of a dataset, optionally for a specific version."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schema retrieved",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ColumnDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Dataset or schema version not found")
    })
    @GetMapping("/{id}/schema")
    public List<ColumnDTO> getSchema(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    description = "Dataset ID",
                    required = true
            )
            @PathVariable("id") String id,
            @Parameter(
                    name = "version",
                    in = ParameterIn.QUERY,
                    description = "Schema version (optional)"
            )
            @RequestParam(name = "version", required = false) Integer version) {

        return metaService.getSchema(id, version);   // return json
    }

    @PostMapping("/{id}/ingestions")
    public ResponseEntity<Void> registerIngestion(@PathVariable String id,
                                                  @RequestBody FileMetadataRequest body) {
        metaService.registerIngestion(id, body);   // see next section
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "List all datasets",
            description = "Returns a list of all registered datasets."
    )
    @ApiResponse(responseCode = "200", description = "List of datasets",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DataSetDTO.class))))
    @GetMapping
    public List<DataSetDTO> list() {
        return metaService.listAllDatasets();
    }
}

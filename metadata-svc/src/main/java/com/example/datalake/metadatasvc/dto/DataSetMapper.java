package com.example.datalake.metadatasvc.dto;

import com.example.datalake.metadatasvc.model.DataSet;

public final class DataSetMapper {
    private DataSetMapper() {}

    public static DataSetDTO toDTO(DataSet entity) {
        return DataSetDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .storageUri(entity.getStorageUri())
                .format(entity.getFormat())
                .project(entity.getProject())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .lastCommitId(entity.getLastCommitId())
                .build();
    }
}

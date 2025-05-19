package com.example.datalake.metadatasvc.service;

import com.example.datalake.metadatasvc.dto.ColumnDTO;
import com.example.datalake.metadatasvc.dto.DataSetDTO;
import com.example.datalake.metadatasvc.dto.FileMetadataRequest;
import com.example.datalake.metadatasvc.dto.NewDataSetDTO;

import java.util.List;

public interface MetaService {
    /** 注册数据集（幂等）*/
    DataSetDTO registerDataSet(NewDataSetDTO dto, String operator);

    /** 提交一次 Schema 变更 */
    void commitSchema(String datasetId, List<ColumnDTO> columns, String message, String operator);

    /** 查询列定义（version 为空返回最新）*/
    List<ColumnDTO> getSchema(String datasetId, Integer version);

    void registerIngestion(String id, FileMetadataRequest body);

    List<DataSetDTO> listAllDatasets();
}

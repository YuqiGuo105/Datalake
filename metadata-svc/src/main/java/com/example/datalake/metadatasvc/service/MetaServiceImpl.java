package com.example.datalake.metadatasvc.service;

import com.example.datalake.metadatasvc.dto.*;
import com.example.datalake.metadatasvc.model.Commit;
import com.example.datalake.metadatasvc.model.DataSet;
import com.example.datalake.metadatasvc.model.DataSetColumn;
import com.example.datalake.metadatasvc.repo.CommitRepository;
import com.example.datalake.metadatasvc.repo.DataSetColumnRepository;
import com.example.datalake.metadatasvc.repo.DataSetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaServiceImpl implements MetaService{
    private final DataSetRepository dataSetRepo;
    private final CommitRepository commitRepo;
    private final DataSetColumnRepository columnRepo;

    @Transactional
    @Override
    public DataSetDTO registerDataSet(NewDataSetDTO dto, String operator) {

        DataSet entity = dataSetRepo.findByName(dto.getName())
                .orElseGet(() -> {
                    DataSet ds = new DataSet();
                    BeanUtils.copyProperties(dto, ds);
                    ds.setCreatedBy(operator);
                    return dataSetRepo.save(ds);
                });

        return DataSetMapper.toDTO(entity);
    }

    @Transactional
    @Override
    public void commitSchema(String datasetId,
                             List<ColumnDTO> columns,
                             String message,
                             String operator) {

        DataSet ds = dataSetRepo.findById(datasetId)
                .orElseThrow(() -> new EntityNotFoundException("dataset not found"));

        int nextVersion = columnRepo.findMaxVersion(datasetId) == null
                ? 1
                : columnRepo.findMaxVersion(datasetId) + 1;

        columns.forEach(c -> {
            DataSetColumn col = new DataSetColumn();
            col.setDataset(ds);
            col.setVersion(nextVersion);
            BeanUtils.copyProperties(c, col);
            columnRepo.save(col);
        });

        Commit commit = new Commit();
        commit.setDatasetId(datasetId);
        commit.setVersion(nextVersion);
        commit.setAuthor(operator);
        commit.setMessage(message);
        commitRepo.save(commit);

        /* 回写最新 commit id */
        ds.setLastCommitId(commit.getId());
        dataSetRepo.save(ds);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ColumnDTO> getSchema(String datasetId, Integer version) {

        /* 若未传版本，拉最新 */
        Integer targetVer = (version == null)
                ? columnRepo.findMaxVersion(datasetId)
                : version;

        if (targetVer == null) {
            throw new EntityNotFoundException("schema not found");
        }

        return columnRepo.findByDataset_IdAndVersionOrderById(datasetId, targetVer)
                .stream()
                .map(col -> {
                    ColumnDTO dto = new ColumnDTO();
                    BeanUtils.copyProperties(col, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Registers the arrival of a new data-file by creating one Commit row
     * and updating the owning DataSet’s lastCommitId reference.
     *
     * @param datasetId UUID string of the target DataSet
     * @param req       payload coming from ingestion-svc
     * @throws IllegalArgumentException if no DataSet exists for the id
     */
    @Transactional
    @Override
    public void registerIngestion(String datasetId, FileMetadataRequest req) {

        /* -------- 1. Ensure the DataSet exists -------- */
        DataSet dataSet = dataSetRepo.findById(datasetId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Dataset " + datasetId + " not found"));

        /* -------- 2. Calculate next sequential version -------- */
        Integer lastVersion = commitRepo
                .findTopByDatasetIdOrderByVersionDesc(datasetId)  // may return null
                .map(Commit::getVersion)
                .orElse(null);
        int nextVersion = lastVersion == null ? 1 : lastVersion + 1;

        /* -------- 3. Persist the Commit -------- */
        Commit c  = new Commit();
        c.setDatasetId(datasetId);
        c.setVersion(nextVersion);
        c.setCommitTime(req.ingestedAt());
        c.setAuthor(req.userId());      // real user
        c.setMessage("Auto-ingest " + req.fileName());
        c.setExtraMeta(Map.of(
                "fileName",  req.fileName(),
                "path",      req.path(),
                "sizeBytes", req.sizeBytes()
        ));

        commitRepo.save(c);

        /* -------- 4. Update DataSet aggregate -------- */
        dataSet.setLastCommitId(c.getId());
        dataSet.setStorageUri(req.path());
        dataSetRepo.save(dataSet);
    }

    @Override
    public List<DataSetDTO> listAllDatasets() {
        List<DataSetDTO> result = new ArrayList<>();
        for (DataSet dataSet: dataSetRepo.findAll())
            result.add(DataSetMapper.toDTO(dataSet));
        return result;
    }
}

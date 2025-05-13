package com.example.datalake.metadatasvc.service;

import com.example.datalake.metadatasvc.dto.ColumnDTO;
import com.example.datalake.metadatasvc.dto.DataSetDTO;
import com.example.datalake.metadatasvc.dto.DataSetMapper;
import com.example.datalake.metadatasvc.dto.NewDataSetDTO;
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

    @Override
    public List<DataSetDTO> listAllDatasets() {
        List<DataSetDTO> result = new ArrayList<>();
        for (DataSet dataSet: dataSetRepo.findAll())
            result.add(DataSetMapper.toDTO(dataSet));
        return result;
    }
}

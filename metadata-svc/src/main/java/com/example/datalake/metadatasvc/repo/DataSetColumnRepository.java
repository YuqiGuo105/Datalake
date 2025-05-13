package com.example.datalake.metadatasvc.repo;

import com.example.datalake.metadatasvc.model.DataSetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSetColumnRepository extends JpaRepository<DataSetColumn, Long> {
    List<DataSetColumn> findByDataset_IdAndVersionOrderById(String datasetId, Integer version);
    @Query("""
           select max(dc.version) 
             from DataSetColumn dc 
            where dc.dataset.id = ?1
           """)
    Integer findMaxVersion(String datasetId);
}

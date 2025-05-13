package com.example.datalake.metadatasvc.repo;

import com.example.datalake.metadatasvc.model.DataSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DataSetRepository extends JpaRepository<DataSet, String> {
    Optional<DataSet> findByName(String name);
    boolean existsByName(String name);
}

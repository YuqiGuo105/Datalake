package com.example.datalake.metadatasvc.repo;

import com.example.datalake.metadatasvc.model.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CommitRepository extends JpaRepository<Commit, String> {
    Optional<Commit> findFirstByDatasetIdOrderByVersionDesc(String datasetId);
    @Query("select c.version from Commit c where c.id = ?1")
    Integer findVersionById(String commitId);
}

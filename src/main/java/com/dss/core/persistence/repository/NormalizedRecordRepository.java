package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.NormalizedRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NormalizedRecordRepository extends JpaRepository<NormalizedRecordEntity, Long> {
    Optional<NormalizedRecordEntity> findByRecordId(String recordId);
    List<NormalizedRecordEntity> findBySourceId(String sourceId);
    List<NormalizedRecordEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<NormalizedRecordEntity> findByStatus(String status);
}

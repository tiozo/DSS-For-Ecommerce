package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.NormalizedRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NormalizedRecordRepository extends JpaRepository<NormalizedRecordEntity, Long> {
    
    @Query("SELECT r FROM NormalizedRecordEntity r WHERE r.tenantId = :tenantId AND r.recordId = :recordId")
    Optional<NormalizedRecordEntity> findByRecordId(String tenantId, String recordId);
    
    @Query("SELECT r FROM NormalizedRecordEntity r WHERE r.tenantId = :tenantId AND r.sourceId = :sourceId")
    List<NormalizedRecordEntity> findBySourceId(String tenantId, String sourceId);
    
    @Query("SELECT r FROM NormalizedRecordEntity r WHERE r.tenantId = :tenantId AND r.timestamp BETWEEN :start AND :end")
    List<NormalizedRecordEntity> findByTimestampBetween(String tenantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT r FROM NormalizedRecordEntity r WHERE r.tenantId = :tenantId AND r.status = :status")
    List<NormalizedRecordEntity> findByStatus(String tenantId, String status);
    
    @Query("SELECT r FROM NormalizedRecordEntity r WHERE r.tenantId = :tenantId")
    List<NormalizedRecordEntity> findAllByTenant(String tenantId);
}

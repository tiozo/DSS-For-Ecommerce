package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DecisionInsightRepository extends JpaRepository<DecisionInsightEntity, Long> {
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.tenantId = :tenantId AND d.recordId = :recordId")
    List<DecisionInsightEntity> findByRecordId(String tenantId, Long recordId);
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.tenantId = :tenantId AND d.status = :status")
    List<DecisionInsightEntity> findByStatus(String tenantId, DecisionInsightEntity.InsightStatus status);
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.tenantId = :tenantId AND d.severity = :severity")
    List<DecisionInsightEntity> findBySeverity(String tenantId, DecisionInsightEntity.Severity severity);
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.tenantId = :tenantId AND d.createdAt BETWEEN :start AND :end")
    List<DecisionInsightEntity> findByCreatedAtBetween(String tenantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.tenantId = :tenantId AND d.ruleName = :ruleName")
    List<DecisionInsightEntity> findByRuleName(String tenantId, String ruleName);
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.tenantId = :tenantId")
    List<DecisionInsightEntity> findAllByTenant(String tenantId);
}

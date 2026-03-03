package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DecisionInsightRepository extends JpaRepository<DecisionInsightEntity, Long> {
    List<DecisionInsightEntity> findByRecordId(Long recordId);
    
    @Query("SELECT d FROM DecisionInsightEntity d WHERE d.status = 'OPEN'")
    List<DecisionInsightEntity> findByStatus(@Param("status") DecisionInsightEntity.InsightStatus status);
    
    List<DecisionInsightEntity> findBySeverity(DecisionInsightEntity.Severity severity);
    List<DecisionInsightEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<DecisionInsightEntity> findByRuleName(String ruleName);
}

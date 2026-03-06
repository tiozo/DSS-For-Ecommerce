package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.DecisionActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionActionRepository extends JpaRepository<DecisionActionEntity, Long> {
    
    @Query("SELECT a FROM DecisionActionEntity a WHERE a.tenantId = :tenantId AND a.recordId = :recordId")
    List<DecisionActionEntity> findByRecordId(String tenantId, Long recordId);
    
    @Query("SELECT a FROM DecisionActionEntity a WHERE a.tenantId = :tenantId AND a.status = :status")
    List<DecisionActionEntity> findByStatus(String tenantId, DecisionActionEntity.ActionStatus status);
    
    @Query("SELECT a FROM DecisionActionEntity a WHERE a.tenantId = :tenantId")
    List<DecisionActionEntity> findAllByTenant(String tenantId);
}

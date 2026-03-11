package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.DynamicRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicRuleRepository extends JpaRepository<DynamicRuleEntity, Long> {

    @Query("SELECT r FROM DynamicRuleEntity r WHERE r.tenantId = :tenantId AND r.name = :name")
    Optional<DynamicRuleEntity> findByName(@Param("tenantId") String tenantId, @Param("name") String name);

    @Query("SELECT r FROM DynamicRuleEntity r WHERE r.tenantId = :tenantId AND r.enabled = true")
    List<DynamicRuleEntity> findEnabledRules(@Param("tenantId") String tenantId);

    @Query("SELECT r FROM DynamicRuleEntity r WHERE r.tenantId = :tenantId")
    List<DynamicRuleEntity> findAllByTenant(@Param("tenantId") String tenantId);
}

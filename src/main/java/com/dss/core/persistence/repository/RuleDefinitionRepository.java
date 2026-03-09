package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.RuleDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleDefinitionRepository extends JpaRepository<RuleDefinitionEntity, Long> {
    List<RuleDefinitionEntity> findByActiveTrue();
}

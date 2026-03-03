package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.ActionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLogEntity, Long> {
    List<ActionLogEntity> findByInsightId(Long insightId);
    List<ActionLogEntity> findByActionType(ActionLogEntity.ActionType actionType);
    List<ActionLogEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

package com.dss.core.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "action_logs", indexes = {
    @Index(name = "idx_insight_id", columnList = "insight_id"),
    @Index(name = "idx_action_type", columnList = "action_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long insightId; // FK to DecisionInsightEntity
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType; // APPROVE, OVERRIDE, ARCHIVE
    
    @Column
    private String userId; // User who performed the action
    
    @Column(columnDefinition = "jsonb")
    private String overrideData; // If action is OVERRIDE, store the override values
    
    @Column(columnDefinition = "text")
    private String reason; // Why the action was taken
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum ActionType {
        APPROVE, OVERRIDE, ARCHIVE
    }
}

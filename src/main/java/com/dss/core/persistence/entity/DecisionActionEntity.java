package com.dss.core.persistence.entity;

import com.dss.core.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "decision_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionActionEntity {
    
    public enum ActionType {
        UP_PRICE, LOGISTICS_OVERLOAD, CUSTOM
    }
    
    public enum ActionStatus {
        PENDING, APPROVED, REJECTED, EXECUTED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private Long recordId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;
    
    @Column(columnDefinition = "text")
    private String payload;
    
    @Column(columnDefinition = "text")
    private String reason;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onPersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

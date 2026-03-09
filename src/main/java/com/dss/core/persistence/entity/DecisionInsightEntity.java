package com.dss.core.persistence.entity;

import com.dss.core.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "decision_insights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionInsightEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private Long recordId;
    
    @Column(nullable = false)
    private String ruleName;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InsightType insightType;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(nullable = false, columnDefinition = "text")
    private String message;
    
    @Column(columnDefinition = "text")
    private String metadata;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InsightStatus status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    @PrePersist
    protected void onPersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
    }
    
    public enum InsightType {
        THRESHOLD, ANOMALY, TREND, CUSTOM
    }
    
    public enum Severity {
        INFO, WARNING, CRITICAL
    }
    
    public enum InsightStatus {
        OPEN, APPROVED, ARCHIVED
    }
}

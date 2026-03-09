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
@Table(name = "dynamic_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicRuleEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "text", nullable = false)
    private String expression;
    
    @Column(columnDefinition = "text")
    private String description;
    
    @Column(nullable = false)
    private Boolean enabled;
    
    @Column(columnDefinition = "text")
    private String actionPayload;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onPersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
}

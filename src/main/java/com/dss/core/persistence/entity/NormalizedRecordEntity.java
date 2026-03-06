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
@Table(name = "normalized_records", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_source_id", columnList = "source_id"),
    @Index(name = "idx_record_id", columnList = "record_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_tenant_record", columnList = "tenant_id,record_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_record_id", columnNames = {"tenant_id", "record_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedRecordEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String sourceId;
    
    @Column(nullable = false)
    private String recordId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "text", nullable = false)
    private String data;
    
    @Column(nullable = false)
    private String status;
    
    @Column(columnDefinition = "text")
    private String metadata;
    
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
}

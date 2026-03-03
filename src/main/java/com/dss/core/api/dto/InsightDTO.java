package com.dss.core.api.dto;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDTO {
    private Long id;
    private Long recordId;
    private String ruleName;
    private String insightType;
    private String severity;
    private String message;
    private String metadata;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static InsightDTO fromEntity(DecisionInsightEntity entity) {
        return InsightDTO.builder()
            .id(entity.getId())
            .recordId(entity.getRecordId())
            .ruleName(entity.getRuleName())
            .insightType(entity.getInsightType().name())
            .severity(entity.getSeverity().name())
            .message(entity.getMessage())
            .metadata(entity.getMetadata())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}

package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * ThresholdRule: Triggers when a numeric field exceeds/falls below a threshold.
 * Example: Alert if "temperature" > 100 or "stock_level" < 10
 */
@Slf4j
public class ThresholdRule implements Rule {
    
    private final String fieldName;
    private final double threshold;
    private final ThresholdType thresholdType; // GREATER_THAN, LESS_THAN
    private final ObjectMapper objectMapper;
    private boolean enabled = true;
    
    public enum ThresholdType {
        GREATER_THAN, LESS_THAN
    }
    
    public ThresholdRule(String fieldName, double threshold, ThresholdType thresholdType) {
        this.fieldName = fieldName;
        this.threshold = threshold;
        this.thresholdType = thresholdType;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public Optional<DecisionInsightEntity> evaluate(NormalizedRecordEntity record) {
        try {
            Map<String, Object> data = objectMapper.readValue(record.getData(), Map.class);
            Object value = data.get(fieldName);
            
            if (value == null) {
                return Optional.empty();
            }
            
            double numValue = Double.parseDouble(value.toString());
            boolean triggered = false;
            
            if (thresholdType == ThresholdType.GREATER_THAN && numValue > threshold) {
                triggered = true;
            } else if (thresholdType == ThresholdType.LESS_THAN && numValue < threshold) {
                triggered = true;
            }
            
            if (triggered) {
                String message = String.format(
                    "Field '%s' value %.2f %s threshold %.2f",
                    fieldName, numValue,
                    thresholdType == ThresholdType.GREATER_THAN ? "exceeds" : "falls below",
                    threshold
                );
                
                DecisionInsightEntity insight = DecisionInsightEntity.builder()
                    .recordId(record.getId())
                    .ruleName(getRuleName())
                    .insightType(getRuleType())
                    .severity(DecisionInsightEntity.Severity.WARNING)
                    .message(message)
                    .metadata(String.format("{\"field\":\"%s\",\"value\":%.2f,\"threshold\":%.2f}", 
                        fieldName, numValue, threshold))
                    .status(DecisionInsightEntity.InsightStatus.OPEN)
                    .build();
                
                return Optional.of(insight);
            }
            
        } catch (Exception e) {
            log.error("Error evaluating threshold rule for field: {}", fieldName, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public String getRuleName() {
        return String.format("THRESHOLD_%s_%s_%.0f", fieldName.toUpperCase(), thresholdType, threshold);
    }
    
    @Override
    public DecisionInsightEntity.InsightType getRuleType() {
        return DecisionInsightEntity.InsightType.THRESHOLD;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

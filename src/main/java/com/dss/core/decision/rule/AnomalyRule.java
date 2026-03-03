package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * AnomalyRule: Detects anomalies based on deviation from expected range.
 * Example: Alert if "price" deviates more than 2 standard deviations from mean.
 * 
 * Note: This is a simplified implementation. Production use would require
 * historical data analysis or ML models.
 */
@Slf4j
public class AnomalyRule implements Rule {
    
    private final String fieldName;
    private final double expectedMin;
    private final double expectedMax;
    private final ObjectMapper objectMapper;
    private boolean enabled = true;
    
    /**
     * @param fieldName Field to monitor
     * @param expectedMin Minimum expected value
     * @param expectedMax Maximum expected value
     */
    public AnomalyRule(String fieldName, double expectedMin, double expectedMax) {
        this.fieldName = fieldName;
        this.expectedMin = expectedMin;
        this.expectedMax = expectedMax;
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
            
            if (numValue < expectedMin || numValue > expectedMax) {
                String message = String.format(
                    "Anomaly detected: '%s' value %.2f outside expected range [%.2f, %.2f]",
                    fieldName, numValue, expectedMin, expectedMax
                );
                
                DecisionInsightEntity insight = DecisionInsightEntity.builder()
                    .recordId(record.getId())
                    .ruleName(getRuleName())
                    .insightType(getRuleType())
                    .severity(DecisionInsightEntity.Severity.WARNING)
                    .message(message)
                    .metadata(String.format(
                        "{\"field\":\"%s\",\"value\":%.2f,\"min\":%.2f,\"max\":%.2f}",
                        fieldName, numValue, expectedMin, expectedMax
                    ))
                    .status(DecisionInsightEntity.InsightStatus.OPEN)
                    .build();
                
                return Optional.of(insight);
            }
            
        } catch (Exception e) {
            log.error("Error evaluating anomaly rule for field: {}", fieldName, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public String getRuleName() {
        return String.format("ANOMALY_%s_[%.0f-%.0f]", fieldName.toUpperCase(), expectedMin, expectedMax);
    }
    
    @Override
    public DecisionInsightEntity.InsightType getRuleType() {
        return DecisionInsightEntity.InsightType.ANOMALY;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

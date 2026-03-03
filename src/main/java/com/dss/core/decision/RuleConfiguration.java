package com.dss.core.decision;

import com.dss.core.decision.rule.RuleEngine;
import com.dss.core.decision.rule.ThresholdRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Slf4j
public class RuleConfiguration {
    
    private final RuleEngine ruleEngine;
    
    public RuleConfiguration(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }
    
    @PostConstruct
    public void initializeRules() {
        log.info("Initializing decision rules...");
        
        // High quantity orders (> 50 units)
        ruleEngine.registerRule(
            new ThresholdRule("QUANTITYORDERED", 50.0, ThresholdRule.ThresholdType.GREATER_THAN)
        );
        
        // High value orders (> 5000 sales)
        ruleEngine.registerRule(
            new ThresholdRule("SALES", 5000.0, ThresholdRule.ThresholdType.GREATER_THAN)
        );
        
        // Low price items (< 20)
        ruleEngine.registerRule(
            new ThresholdRule("PRICEEACH", 20.0, ThresholdRule.ThresholdType.LESS_THAN)
        );
        
        log.info("Rules initialized. Total rules: {}", ruleEngine.getRegisteredRules().size());
    }
}

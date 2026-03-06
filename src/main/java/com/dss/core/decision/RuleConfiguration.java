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
        log.info("Rules initialized. Total rules: {}", ruleEngine.getRegisteredRules().size());
    }
}

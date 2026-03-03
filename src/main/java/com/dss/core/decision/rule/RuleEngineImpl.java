package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RuleEngineImpl implements RuleEngine {
    
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
    
    @Override
    public void registerRule(Rule rule) {
        if (rule == null || rule.getRuleName() == null) {
            throw new IllegalArgumentException("Rule and rule name cannot be null");
        }
        rules.put(rule.getRuleName(), rule);
        log.info("Registered rule: {}", rule.getRuleName());
    }
    
    @Override
    public void unregisterRule(String ruleName) {
        rules.remove(ruleName);
        log.info("Unregistered rule: {}", ruleName);
    }
    
    @Override
    public List<DecisionInsightEntity> executeRules(NormalizedRecordEntity record) {
        List<DecisionInsightEntity> insights = new ArrayList<>();
        
        for (Rule rule : rules.values()) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            try {
                Optional<DecisionInsightEntity> insight = rule.evaluate(record);
                insight.ifPresent(insights::add);
            } catch (Exception e) {
                log.error("Error executing rule: {}", rule.getRuleName(), e);
            }
        }
        
        return insights;
    }
    
    @Override
    public List<Rule> getRegisteredRules() {
        return new ArrayList<>(rules.values());
    }
}

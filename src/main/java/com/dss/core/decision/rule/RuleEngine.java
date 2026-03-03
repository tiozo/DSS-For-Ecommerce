package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;

import java.util.List;

/**
 * RuleEngine: Decoupled service that executes a set of rules against records.
 * Supports dynamic rule registration and execution without core code changes.
 */
public interface RuleEngine {
    
    /**
     * Register a new rule
     */
    void registerRule(Rule rule);
    
    /**
     * Unregister a rule by name
     */
    void unregisterRule(String ruleName);
    
    /**
     * Execute all active rules against a record
     * @param record The record to evaluate
     * @return List of insights generated (empty if no rules triggered)
     */
    List<DecisionInsightEntity> executeRules(NormalizedRecordEntity record);
    
    /**
     * Get all registered rules
     */
    List<Rule> getRegisteredRules();
}

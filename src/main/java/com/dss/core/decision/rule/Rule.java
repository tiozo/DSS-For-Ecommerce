package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;

import java.util.Optional;

/**
 * Specification Pattern: Each rule is a self-contained specification
 * that can evaluate a record and generate an insight if triggered.
 */
public interface Rule {
    
    /**
     * Evaluate the record against this rule.
     * @param record The normalized record to evaluate
     * @return Optional containing DecisionInsight if rule triggers, empty otherwise
     */
    Optional<DecisionInsightEntity> evaluate(NormalizedRecordEntity record);
    
    /**
     * Get the rule name for identification
     */
    String getRuleName();
    
    /**
     * Get the rule type
     */
    DecisionInsightEntity.InsightType getRuleType();
    
    /**
     * Check if this rule is enabled
     */
    boolean isEnabled();
}

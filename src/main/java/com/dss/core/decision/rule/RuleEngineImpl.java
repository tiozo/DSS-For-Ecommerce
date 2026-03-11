package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.dss.core.persistence.repository.DynamicRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RuleEngineImpl implements RuleEngine {

    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
    private final DynamicRuleRepository dynamicRuleRepository;
    private final SpELRuleEvaluator spelEvaluator;

    public RuleEngineImpl(DynamicRuleRepository dynamicRuleRepository, SpELRuleEvaluator spelEvaluator) {
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.spelEvaluator = spelEvaluator;
    }

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

        // Execute static rules
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

        // Execute dynamic rules from database
        List<DynamicRuleEntity> dynamicRules = dynamicRuleRepository.findEnabledRules(record.getTenantId());
        log.debug("[DEBUG - ENGINE] Evaluating record ID: {} against {} dynamic rules.", record.getId(),
                dynamicRules.size());
        if (dynamicRules.isEmpty()) {
            log.warn("[WARNING - ENGINE] Record {} evaluated, but NO active dynamic rules found for tenant!",
                    record.getId());
        }
        for (DynamicRuleEntity dynamicRule : dynamicRules) {
            try {
                DecisionInsightEntity.Severity insightSeverity = DecisionInsightEntity.Severity.INFO;
                if (dynamicRule.getSeverity() != null) {
                    insightSeverity = dynamicRule.getSeverity();
                }
                if (spelEvaluator.evaluate(dynamicRule, record)) {
                    DecisionInsightEntity insight = DecisionInsightEntity.builder()
                            .tenantId(record.getTenantId())
                            .recordId(record.getId())
                            .ruleName(dynamicRule.getName())
                            .insightType(DecisionInsightEntity.InsightType.CUSTOM)
                            .severity(insightSeverity)
                            .message(dynamicRule.getDescription())
                            .status(DecisionInsightEntity.InsightStatus.OPEN)
                            .build();
                    insights.add(insight);
                }
            } catch (Exception e) {
                log.error("[CRITICAL - SPEL] Failed to evaluate rule '{}' on record {}. Expression: '{}'. Error: {}",
                        dynamicRule.getName(), record.getId(), dynamicRule.getExpression(), e.getMessage());
            }
        }

        return insights;
    }

    @Override
    public List<Rule> getRegisteredRules() {
        return new ArrayList<>(rules.values());
    }
}

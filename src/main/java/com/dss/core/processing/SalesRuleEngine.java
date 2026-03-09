package com.dss.core.processing;

import com.dss.core.persistence.entity.*;
import com.dss.core.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesRuleEngine {
    
    private final SalesRecordRepository salesRepository;
    private final RuleDefinitionRepository ruleRepository;
    private final DecisionInsightRepository insightRepository;
    
    public List<DecisionInsightEntity> processRules() {
        List<DecisionInsightEntity> insights = new ArrayList<>();
        List<RuleDefinitionEntity> rules = ruleRepository.findByActiveTrue();
        List<SalesRecordEntity> sales = salesRepository.findAll();
        
        for (RuleDefinitionEntity rule : rules) {
            insights.addAll(evaluateRule(rule, sales));
        }
        
        return insightRepository.saveAll(insights);
    }
    
    private List<DecisionInsightEntity> evaluateRule(RuleDefinitionEntity rule, List<SalesRecordEntity> sales) {
        List<DecisionInsightEntity> insights = new ArrayList<>();
        
        if ("threshold".equalsIgnoreCase(rule.getRuleType())) {
            for (SalesRecordEntity sale : sales) {
                if (sale.getSales() != null && rule.getThreshold() != null) {
                    BigDecimal threshold = new BigDecimal(rule.getThreshold());
                    if (sale.getSales().compareTo(threshold) > 0) {
                        insights.add(createInsight(rule, sale, "High value sale detected"));
                    }
                }
            }
        }
        
        return insights;
    }
    
    private DecisionInsightEntity createInsight(RuleDefinitionEntity rule, SalesRecordEntity sale, String message) {
        return DecisionInsightEntity.builder()
            .tenantId("default-tenant")
            .recordId(sale.getId())
            .ruleName(rule.getRuleName())
            .insightType(DecisionInsightEntity.InsightType.THRESHOLD)
            .severity(DecisionInsightEntity.Severity.WARNING)
            .message(message + ": Order " + sale.getOrderNumber())
            .status(DecisionInsightEntity.InsightStatus.OPEN)
            .build();
    }
}

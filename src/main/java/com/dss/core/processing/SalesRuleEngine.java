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
                if (evaluateThresholdRule(rule, sale)) {
                    insights.add(createInsight(rule, sale, determineMessage(rule, sale)));
                }
            }
        }
        
        return insights;
    }
    
    private boolean evaluateThresholdRule(RuleDefinitionEntity rule, SalesRecordEntity sale) {
        if (rule.getThreshold() == null || rule.getCondition() == null) return false;
        
        try {
            BigDecimal threshold = new BigDecimal(rule.getThreshold());
            
            // Check sales field
            if (rule.getCondition().contains("sales") && sale.getSales() != null) {
                return sale.getSales().compareTo(threshold) > 0;
            }
            
            // Check quantityOrdered field
            if (rule.getCondition().contains("quantityOrdered") && sale.getQuantityOrdered() != null) {
                return new BigDecimal(sale.getQuantityOrdered()).compareTo(threshold) > 0;
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return false;
    }
    
    private String determineMessage(RuleDefinitionEntity rule, SalesRecordEntity sale) {
        if (rule.getCondition().contains("sales")) {
            return String.format("%s: Order #%d - $%.2f", 
                rule.getRuleName(), sale.getOrderNumber(), sale.getSales());
        } else if (rule.getCondition().contains("quantityOrdered")) {
            return String.format("%s: Order #%d - Qty: %d", 
                rule.getRuleName(), sale.getOrderNumber(), sale.getQuantityOrdered());
        }
        return rule.getRuleName() + ": Order " + sale.getOrderNumber();
    }
    
    private DecisionInsightEntity createInsight(RuleDefinitionEntity rule, SalesRecordEntity sale, String message) {
        return DecisionInsightEntity.builder()
            .tenantId("default")
            .recordId(sale.getId())
            .ruleName(rule.getRuleName())
            .insightType(DecisionInsightEntity.InsightType.THRESHOLD)
            .severity(DecisionInsightEntity.Severity.WARNING)
            .message(message + ": Order " + sale.getOrderNumber())
            .status(DecisionInsightEntity.InsightStatus.OPEN)
            .build();
    }
}

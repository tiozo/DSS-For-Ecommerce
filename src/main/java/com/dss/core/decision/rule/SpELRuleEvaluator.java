package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SpELRuleEvaluator: Evaluates dynamic rules using Spring Expression Language
 */
@Component
@Slf4j
public class SpELRuleEvaluator {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ObjectMapper objectMapper;
    
    public SpELRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public boolean evaluate(DynamicRuleEntity rule, NormalizedRecordEntity record) {
        try {
            Map<String, Object> data = objectMapper.readValue(record.getData(), Map.class);
            Expression expr = parser.parseExpression(rule.getExpression());
            Boolean result = expr.getValue(data, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            log.error("Error evaluating rule: {} for record: {}", rule.getName(), record.getRecordId(), e);
            return false;
        }
    }
}

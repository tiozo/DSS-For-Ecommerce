package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SpELRuleEvaluator: Evaluates dynamic rules using Spring Expression Language.
 *
 * <p>
 * Expressions use the {@code #record} variable to access sales record fields,
 * e.g. {@code #record.sales > 5000} or {@code #record.status == 'Cancelled'}.
 * A {@link MapAccessor} is registered so dot-notation resolves Map keys
 * directly.
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
            // Parse the stored JSON data into a plain Map
            Map<String, Object> dataMap = objectMapper.readValue(record.getData(), Map.class);

            // Build a context that:
            // 1. Has a MapAccessor so #record.sales resolves to dataMap.get("sales")
            // 2. Exposes the map as the named variable #record
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.addPropertyAccessor(new MapAccessor());
            context.setVariable("record", dataMap);

            log.debug("[DEBUG - SPEL] Evaluating expression '{}' for record {}",
                    rule.getExpression(), record.getRecordId());

            Expression expr = parser.parseExpression(rule.getExpression());
            Boolean result = expr.getValue(context, Boolean.class);

            log.debug("[DEBUG - SPEL] Result for '{}': {}", rule.getName(), result);
            return result != null && result;

        } catch (Exception e) {
            log.error("[CRITICAL - SPEL] Failed to evaluate rule '{}' on record {}. Expression: '{}'. Error: {}",
                    rule.getName(), record.getRecordId(), rule.getExpression(), e.getMessage());
            return false;
        }
    }
}

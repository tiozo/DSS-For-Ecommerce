package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedCaseInsensitiveMap; // <--- CỨU CÁNH CHÍNH LÀ ĐÂY

import java.util.Map;

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
            Map<String, Object> rawData = objectMapper.readValue(record.getData(),
                    new TypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> safeData = new LinkedCaseInsensitiveMap<>();
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                Object val = entry.getValue();

                if (val instanceof String) {
                    try {
                        val = Double.parseDouble(val.toString().replace(",", ""));
                    } catch (NumberFormatException ignored) {
                    }
                }
                safeData.put(entry.getKey(), val);
            }

            StandardEvaluationContext context = new StandardEvaluationContext();
            context.addPropertyAccessor(new MapAccessor());
            context.setVariable("record", safeData);

            Expression expr = parser.parseExpression(rule.getExpression());
            Boolean result = expr.getValue(context, Boolean.class);

            return result != null && result;

        } catch (Exception e) {
            System.err.println("❌ [CRITICAL SPEL ERROR]");
            System.err.println(" - Lỗi tại Rule: " + rule.getName());
            System.err.println(" - Biểu thức (SpEL): " + rule.getExpression());
            System.err.println(" - Dữ liệu đang test: " + record.getData());
            System.err.println(" - NGUYÊN NHÂN: " + e.getMessage());
            System.err.println("----------------------------------------");
            return false;
        }
    }
}
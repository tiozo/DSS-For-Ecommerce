package com.dss.core.processing;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.repository.DynamicRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.util.*;
import com.dss.core.tenant.TenantContext;


@Slf4j
@Service
@RequiredArgsConstructor
public class RuleCsvProcessor {

    private final DynamicRuleRepository dynamicRuleRepository;

    public int processCsv(MultipartFile file) throws Exception {
        List<DynamicRuleEntity> rules = new ArrayList<>();

        try (var reader = new InputStreamReader(file.getInputStream())) {
            CSVParser csv = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            for (CSVRecord row : csv) {
                DecisionInsightEntity.Severity severity = DecisionInsightEntity.Severity.INFO;
                if (row.isMapped("severity") && !row.get("severity").isEmpty()) {
                    try {
                        severity = DecisionInsightEntity.Severity.valueOf(row.get("severity").toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid severity '{}' in CSV. Defaulting to INFO.", row.get("severity"));
                    }
                }

                String rawCondition = row.get("condition");
                String thresholdValue = row.isMapped("threshold") ? row.get("threshold") : "";

                String spelExpression = translateToSpEL(rawCondition, thresholdValue);
                log.info("[DEBUG - RULE INGEST] Parsing Rule: '{}' | Translated SpEL: '{}' | Severity: {}",
                        row.get("ruleName"), spelExpression, severity);

                rules.add(DynamicRuleEntity.builder()
                        .name(row.get("ruleName"))
                        .tenantId(TenantContext.getTenantId())
                        .expression(spelExpression)
                        .severity(severity)
                        .actionPayload(row.isMapped("action") ? row.get("action") : null)
                        .description("Rule triggered by threshold: " + thresholdValue)
                        .enabled(true)
                        .build());
            }
        }

        dynamicRuleRepository.saveAll(rules);
        log.info("[DEBUG - RULE INGEST] Successfully saved {} rules to DB.", rules.size());
        return rules.size();
    }

    /**
     * Translates a raw CSV condition into a valid SpEL expression.
     * <p>
     * Examples:
     * "sales > threshold" + threshold="5000" → "#record.sales > 5000"
     * "quantityOrdered < threshold" + threshold="10" → "#record.quantityOrdered <
     * 10"
     * "status == 'Cancelled'" + threshold="" → "#record.status == 'Cancelled'"
     */
    private String translateToSpEL(String rawCondition, String thresholdValue) {
        if (rawCondition == null || rawCondition.isBlank()) {
            return rawCondition;
        }

        String expression = rawCondition.toLowerCase();

        String[] recordFields = {
                "sales", "quantityordered", "priceeach", "status", "msrp", "month_id"
        };

        for (String field : recordFields) {
            expression = expression.replaceAll("\\b" + field + "\\b", "#record." + field);
        }

        if (thresholdValue != null && !thresholdValue.isBlank()) {
            expression = expression.replaceAll("\\bthreshold\\b", thresholdValue);
        }

        log.debug("SpEL translation: [{}] + threshold={} → [{}]", rawCondition, thresholdValue, expression);
        return expression;
    }
}
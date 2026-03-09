package com.dss.core.decision.rule;

import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.repository.DynamicRuleRepository;
import com.dss.core.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

//@Component
@Slf4j
public class RuleLoader implements CommandLineRunner {
    
    private final DynamicRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;
    
    public RuleLoader(DynamicRuleRepository ruleRepository, ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void run(String... args) throws Exception {
        TenantContext.setTenantId("default-tenant");
        
        try (InputStream is = getClass().getResourceAsStream("/rules.json")) {
            if (is == null) {
                log.warn("rules.json not found");
                return;
            }
            
            List<Map<String, Object>> rulesData = objectMapper.readValue(is, List.class);
            
            for (Map<String, Object> ruleData : rulesData) {
                DynamicRuleEntity rule = DynamicRuleEntity.builder()
                    .name((String) ruleData.get("name"))
                    .expression((String) ruleData.get("expression"))
                    .description((String) ruleData.get("description"))
                    .enabled(true)
                    .actionPayload((String) ruleData.get("action"))
                    .build();
                
                ruleRepository.save(rule);
                log.info("Loaded rule: {}", rule.getName());
            }
        } finally {
            TenantContext.clear();
        }
    }
}

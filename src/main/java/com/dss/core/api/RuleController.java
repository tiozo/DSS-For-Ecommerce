package com.dss.core.api;

import com.dss.core.persistence.entity.DynamicRuleEntity;
import com.dss.core.persistence.repository.DynamicRuleRepository;
import com.dss.core.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@Slf4j
public class RuleController {
    
    private final DynamicRuleRepository ruleRepository;
    
    public RuleController(DynamicRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }
    
    @PostMapping
    public ResponseEntity<DynamicRuleEntity> createRule(@RequestBody DynamicRuleEntity rule) {
        String tenantId = TenantContext.getTenantId();
        rule.setTenantId(tenantId);
        DynamicRuleEntity saved = ruleRepository.save(rule);
        log.info("Created rule: {} for tenant: {}", rule.getName(), tenantId);
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping
    public ResponseEntity<List<DynamicRuleEntity>> getAllRules() {
        String tenantId = TenantContext.getTenantId();
        List<DynamicRuleEntity> rules = ruleRepository.findAllByTenant(tenantId);
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/enabled")
    public ResponseEntity<List<DynamicRuleEntity>> getEnabledRules() {
        String tenantId = TenantContext.getTenantId();
        List<DynamicRuleEntity> rules = ruleRepository.findEnabledRules(tenantId);
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DynamicRuleEntity> getRule(@PathVariable Long id) {
        DynamicRuleEntity rule = ruleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        return ResponseEntity.ok(rule);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DynamicRuleEntity> updateRule(@PathVariable Long id, @RequestBody DynamicRuleEntity rule) {
        DynamicRuleEntity existing = ruleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        
        existing.setName(rule.getName());
        existing.setExpression(rule.getExpression());
        existing.setDescription(rule.getDescription());
        existing.setEnabled(rule.getEnabled());
        existing.setActionPayload(rule.getActionPayload());
        
        DynamicRuleEntity updated = ruleRepository.save(existing);
        log.info("Updated rule: {}", id);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleRepository.deleteById(id);
        log.info("Deleted rule: {}", id);
        return ResponseEntity.noContent().build();
    }
}

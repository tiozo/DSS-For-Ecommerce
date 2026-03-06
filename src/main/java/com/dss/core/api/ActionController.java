package com.dss.core.api;

import com.dss.core.persistence.entity.DecisionActionEntity;
import com.dss.core.persistence.repository.DecisionActionRepository;
import com.dss.core.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
@Slf4j
public class ActionController {
    
    private final DecisionActionRepository actionRepository;
    
    public ActionController(DecisionActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }
    
    @PostMapping
    public ResponseEntity<DecisionActionEntity> createAction(@RequestBody DecisionActionRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        DecisionActionEntity action = DecisionActionEntity.builder()
            .tenantId(tenantId)
            .recordId(request.getRecordId())
            .actionType(request.getActionType())
            .status(DecisionActionEntity.ActionStatus.PENDING)
            .payload(request.getPayload())
            .reason(request.getReason())
            .build();
        
        DecisionActionEntity saved = actionRepository.save(action);
        log.info("Created action: {} for record: {}", request.getActionType(), request.getRecordId());
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<DecisionActionEntity>> getPendingActions() {
        String tenantId = TenantContext.getTenantId();
        List<DecisionActionEntity> actions = actionRepository.findByStatus(tenantId, DecisionActionEntity.ActionStatus.PENDING);
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/record/{recordId}")
    public ResponseEntity<List<DecisionActionEntity>> getActionsForRecord(@PathVariable Long recordId) {
        String tenantId = TenantContext.getTenantId();
        List<DecisionActionEntity> actions = actionRepository.findByRecordId(tenantId, recordId);
        return ResponseEntity.ok(actions);
    }
    
    @PutMapping("/{id}/approve")
    public ResponseEntity<DecisionActionEntity> approveAction(@PathVariable Long id) {
        DecisionActionEntity action = actionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Action not found"));
        
        action.setStatus(DecisionActionEntity.ActionStatus.APPROVED);
        DecisionActionEntity updated = actionRepository.save(action);
        log.info("Approved action: {}", id);
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/{id}/reject")
    public ResponseEntity<DecisionActionEntity> rejectAction(@PathVariable Long id) {
        DecisionActionEntity action = actionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Action not found"));
        
        action.setStatus(DecisionActionEntity.ActionStatus.REJECTED);
        DecisionActionEntity updated = actionRepository.save(action);
        log.info("Rejected action: {}", id);
        return ResponseEntity.ok(updated);
    }
    
    public static class DecisionActionRequest {
        private Long recordId;
        private DecisionActionEntity.ActionType actionType;
        private String payload;
        private String reason;
        
        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }
        
        public DecisionActionEntity.ActionType getActionType() { return actionType; }
        public void setActionType(DecisionActionEntity.ActionType actionType) { this.actionType = actionType; }
        
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}

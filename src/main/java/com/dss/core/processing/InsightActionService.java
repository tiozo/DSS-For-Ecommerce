package com.dss.core.processing;

import com.dss.core.persistence.entity.ActionLogEntity;
import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.repository.ActionLogRepository;
import com.dss.core.persistence.repository.DecisionInsightRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * InsightActionService: Handles user actions on insights
 */
@Service
@Slf4j
public class InsightActionService {
    
    private final DecisionInsightRepository insightRepository;
    private final ActionLogRepository actionLogRepository;
    
    public InsightActionService(
            DecisionInsightRepository insightRepository,
            ActionLogRepository actionLogRepository) {
        this.insightRepository = insightRepository;
        this.actionLogRepository = actionLogRepository;
    }
    
    /**
     * Approve an insight (mark as seen)
     */
    @Transactional
    public void approveInsight(Long insightId, String userId, String reason) {
        DecisionInsightEntity insight = insightRepository.findById(insightId)
            .orElseThrow(() -> new IllegalArgumentException("Insight not found: " + insightId));
        
        insight.setStatus(DecisionInsightEntity.InsightStatus.APPROVED);
        insightRepository.save(insight);
        
        ActionLogEntity actionLog = ActionLogEntity.builder()
            .insightId(insightId)
            .actionType(ActionLogEntity.ActionType.APPROVE)
            .userId(userId)
            .reason(reason)
            .build();
        actionLogRepository.save(actionLog);
        
        log.info("Approved insight: {} by user: {}", insightId, userId);
    }
    
    /**
     * Override an insight with manual data
     */
    @Transactional
    public void overrideInsight(Long insightId, String userId, String overrideData, String reason) {
        DecisionInsightEntity insight = insightRepository.findById(insightId)
            .orElseThrow(() -> new IllegalArgumentException("Insight not found: " + insightId));
        
        insight.setStatus(DecisionInsightEntity.InsightStatus.APPROVED);
        insightRepository.save(insight);
        
        ActionLogEntity actionLog = ActionLogEntity.builder()
            .insightId(insightId)
            .actionType(ActionLogEntity.ActionType.OVERRIDE)
            .userId(userId)
            .overrideData(overrideData)
            .reason(reason)
            .build();
        actionLogRepository.save(actionLog);
        
        log.info("Overrode insight: {} by user: {}", insightId, userId);
    }
    
    /**
     * Archive an insight
     */
    @Transactional
    public void archiveInsight(Long insightId, String userId, String reason) {
        DecisionInsightEntity insight = insightRepository.findById(insightId)
            .orElseThrow(() -> new IllegalArgumentException("Insight not found: " + insightId));
        
        insight.setStatus(DecisionInsightEntity.InsightStatus.ARCHIVED);
        insightRepository.save(insight);
        
        ActionLogEntity actionLog = ActionLogEntity.builder()
            .insightId(insightId)
            .actionType(ActionLogEntity.ActionType.ARCHIVE)
            .userId(userId)
            .reason(reason)
            .build();
        actionLogRepository.save(actionLog);
        
        log.info("Archived insight: {} by user: {}", insightId, userId);
    }
}

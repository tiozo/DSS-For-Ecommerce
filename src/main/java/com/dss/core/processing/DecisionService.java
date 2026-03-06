package com.dss.core.processing;

import com.dss.core.decision.rule.RuleEngine;
import com.dss.core.ingestion.NormalizedRecord;
import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.dss.core.persistence.repository.DecisionInsightRepository;
import com.dss.core.persistence.repository.NormalizedRecordRepository;
import com.dss.core.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DecisionService: Orchestrates the complete pipeline
 * Ingest → Normalize → Persist → Execute Rules → Persist Insights
 */
@Service
@Slf4j
public class DecisionService {
    
    private final NormalizedRecordRepository recordRepository;
    private final DecisionInsightRepository insightRepository;
    private final RuleEngine ruleEngine;
    private final ObjectMapper objectMapper;
    
    public DecisionService(
            NormalizedRecordRepository recordRepository,
            DecisionInsightRepository insightRepository,
            RuleEngine ruleEngine,
            ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.insightRepository = insightRepository;
        this.ruleEngine = ruleEngine;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Process a normalized record through the full pipeline
     */
    @Transactional
    public void processRecord(NormalizedRecord normalizedRecord) {
        try {
            String tenantId = TenantContext.getTenantId();
            
            // Step 1: Check if record already exists
            if (recordRepository.findByRecordId(tenantId, normalizedRecord.getRecordId()).isPresent()) {
                log.debug("Record already exists, skipping: {}", normalizedRecord.getRecordId());
                return;
            }
            
            // Step 2: Convert to entity and persist
            NormalizedRecordEntity entity = convertToEntity(normalizedRecord);
            NormalizedRecordEntity savedRecord = recordRepository.save(entity);
            log.debug("Persisted record: {}", savedRecord.getRecordId());
            
            // Step 3: Execute rules
            List<DecisionInsightEntity> insights = ruleEngine.executeRules(savedRecord);
            
            // Step 4: Persist insights
            if (!insights.isEmpty()) {
                insightRepository.saveAll(insights);
                log.info("Generated {} insights for record: {}", insights.size(), savedRecord.getRecordId());
            }
            
        } catch (Exception e) {
            log.error("Error processing record: {}", normalizedRecord.getRecordId(), e);
            throw new RuntimeException("Failed to process record", e);
        }
    }
    
    /**
     * Batch process multiple records
     */
    @Transactional
    public void processRecords(List<NormalizedRecord> records) {
        for (NormalizedRecord record : records) {
            processRecord(record);
        }
    }
    
    /**
     * Convert NormalizedRecord (DTO) to NormalizedRecordEntity (JPA)
     */
    private NormalizedRecordEntity convertToEntity(NormalizedRecord dto) throws Exception {
        return NormalizedRecordEntity.builder()
            .sourceId(dto.getSourceId())
            .recordId(dto.getRecordId())
            .timestamp(dto.getTimestamp())
            .data(objectMapper.writeValueAsString(dto.getData()))
            .status(dto.getStatus())
            .metadata(dto.getMetadata())
            .build();
    }
    
    /**
     * Retrieve insights for a specific record
     */
    public List<DecisionInsightEntity> getInsightsForRecord(Long recordId) {
        return insightRepository.findByRecordId(TenantContext.getTenantId(), recordId);
    }
    
    /**
     * Retrieve open insights
     */
    public List<DecisionInsightEntity> getOpenInsights() {
        return insightRepository.findByStatus(TenantContext.getTenantId(), DecisionInsightEntity.InsightStatus.OPEN);
    }
}

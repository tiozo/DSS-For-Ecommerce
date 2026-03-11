package com.dss.core.api;

import com.dss.core.decision.rule.RuleEngine;
import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.entity.NormalizedRecordEntity;
import com.dss.core.persistence.repository.*;
import com.dss.core.processing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SalesCsvProcessor salesCsvProcessor;
    private final RuleCsvProcessor ruleCsvProcessor;
    private final RuleEngine ruleEngine;
    private final SalesRecordRepository salesRepository;
    private final NormalizedRecordRepository normalizedRepository;
    private final DecisionInsightRepository insightRepository;

    @PostMapping("/upload-sales")
    public ResponseEntity<Map<String, Object>> uploadSales(@RequestParam("file") MultipartFile file) {
        try {
            int count = salesCsvProcessor.processCsv(file);
            log.info("[DEBUG - PIPELINE] Uploaded {} sales records. Triggering Evaluation...", count);

            int insightsGenerated = triggerEvaluation();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recordsProcessed", count,
                    "insightsGenerated", insightsGenerated));
        } catch (Exception e) {
            log.error("Sales upload failed", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/upload-rules")
    public ResponseEntity<Map<String, Object>> uploadRules(@RequestParam("file") MultipartFile file) {
        try {
            int count = ruleCsvProcessor.processCsv(file);
            log.info("[DEBUG - PIPELINE] Uploaded {} rules. Re-evaluating all records...", count);

            int insightsGenerated = triggerEvaluation();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "rulesProcessed", count,
                    "insightsGenerated", insightsGenerated));
        } catch (Exception e) {
            log.error("Rules upload failed", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("salesByMonth", convertToMap(salesRepository.getSalesByMonth()));
        response.put("salesByProductLine", convertToMap(salesRepository.getSalesByProductLine()));
        response.put("statusDistribution", convertToMap(salesRepository.getStatusDistribution()));
        response.put("totalRecords", salesRepository.count());

        var insights = insightRepository.findAll();
        response.put("insightCount", insights.size());
        response.put("hasInsights", !insights.isEmpty());
        response.put("insights", insights);

        return ResponseEntity.ok(response);
    }

    @Transactional
    private int triggerEvaluation() {
        List<NormalizedRecordEntity> records = normalizedRepository.findAll();
        List<DecisionInsightEntity> allInsights = new ArrayList<>();

        for (NormalizedRecordEntity record : records) {
            List<DecisionInsightEntity> generated = ruleEngine.executeRules(record);
            allInsights.addAll(generated);
        }

        if (!allInsights.isEmpty()) {
            insightRepository.saveAll(allInsights);
        }

        log.info("[DEBUG - PIPELINE] Evaluation completed. New insights: {}", allInsights.size());
        return allInsights.size();
    }

    private List<Map<String, Object>> convertToMap(List<Object[]> data) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            result.add(Map.of("label", String.valueOf(row[0]), "value", row[1]));
        }
        return result;
    }
}
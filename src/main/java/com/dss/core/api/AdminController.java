package com.dss.core.api;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.repository.*;
import com.dss.core.processing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final SalesCsvProcessor salesCsvProcessor;
    private final RuleCsvProcessor ruleCsvProcessor;
    private final SalesRuleEngine salesRuleEngine;
    private final SalesRecordRepository salesRepository;
    private final DecisionInsightRepository insightRepository;
    
    @PostMapping("/upload-sales")
    public ResponseEntity<Map<String, Object>> uploadSales(@RequestParam("file") MultipartFile file) {
        try {
            int count = salesCsvProcessor.processCsv(file);
            List<DecisionInsightEntity> insights = salesRuleEngine.processRules();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "recordsProcessed", count,
                "insightsGenerated", insights.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/upload-rules")
    public ResponseEntity<Map<String, Object>> uploadRules(@RequestParam("file") MultipartFile file) {
        try {
            int count = ruleCsvProcessor.processCsv(file);
            return ResponseEntity.ok(Map.of("success", true, "rulesProcessed", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        var salesByMonth = salesRepository.getSalesByMonth();
        var salesByProduct = salesRepository.getSalesByProductLine();
        var statusDist = salesRepository.getStatusDistribution();
        var insights = insightRepository.findAll();
        
        return ResponseEntity.ok(Map.of(
            "salesByMonth", convertToMap(salesByMonth),
            "salesByProductLine", convertToMap(salesByProduct),
            "statusDistribution", convertToMap(statusDist),
            "totalRecords", salesRepository.count(),
            "insightCount", insights.size(),
            "hasInsights", !insights.isEmpty()
        ));
    }
    
    private List<Map<String, Object>> convertToMap(List<Object[]> data) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            result.add(Map.of("label", row[0], "value", row[1]));
        }
        return result;
    }
}

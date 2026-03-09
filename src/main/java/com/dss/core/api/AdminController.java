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
            // Trigger rule evaluation after uploading rules
            List<DecisionInsightEntity> insights = salesRuleEngine.processRules();
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "rulesProcessed", count,
                "insightsGenerated", insights.size()
            ));
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("salesByMonth", convertToMap(salesByMonth));
        response.put("salesByProductLine", convertToMap(salesByProduct));
        response.put("statusDistribution", convertToMap(statusDist));
        response.put("totalRecords", salesRepository.count());
        response.put("insightCount", insights.size());
        response.put("hasInsights", !insights.isEmpty());
        response.put("insights", insights);
        
        return ResponseEntity.ok(response);
    }
    
    private List<Map<String, Object>> convertToMap(List<Object[]> data) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            result.add(Map.of("label", row[0], "value", row[1]));
        }
        return result;
    }
}

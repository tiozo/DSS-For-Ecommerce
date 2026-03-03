package com.dss.core.api;

import com.dss.core.api.dto.InsightActionRequest;
import com.dss.core.api.dto.InsightDTO;
import com.dss.core.ingestion.NormalizedRecord;
import com.dss.core.ingestion.impl.ApiDataIngestor;
import com.dss.core.ingestion.impl.CsvDataIngestor;
import com.dss.core.processing.DataProcessingService;
import com.dss.core.processing.DecisionService;
import com.dss.core.processing.InsightActionService;
import com.dss.core.processing.ProcessingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DssController {
    
    private final DataProcessingService processingService;
    private final CsvDataIngestor csvIngestor;
    private final ApiDataIngestor apiIngestor;
    private final DecisionService decisionService;
    private final InsightActionService insightActionService;
    
    public DssController(DataProcessingService processingService,
                        CsvDataIngestor csvIngestor,
                        ApiDataIngestor apiIngestor,
                        DecisionService decisionService,
                        InsightActionService insightActionService) {
        this.processingService = processingService;
        this.csvIngestor = csvIngestor;
        this.apiIngestor = apiIngestor;
        this.decisionService = decisionService;
        this.insightActionService = insightActionService;
        
        processingService.registerIngestor(csvIngestor);
        processingService.registerIngestor(apiIngestor);
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return Map.of(
                "sources", processingService.getSourceStatus(),
                "recordCount", processingService.getProcessedData().size(),
                "timestamp", System.currentTimeMillis()
        );
    }
    
    @PostMapping("/upload-csv")
    public CompletableFuture<ProcessingResult> uploadCsv(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        csvIngestor.setCsvContent(content);
        return processingService.processSource(csvIngestor.getSourceId());
    }
    
    @PostMapping("/configure-api")
    public Map<String, String> configureApi(@RequestBody ApiConfig config) {
        apiIngestor.setApiEndpoint(config.getEndpoint());
        if (config.getMethod() != null) {
            apiIngestor.setApiMethod(config.getMethod());
        }
        return Map.of("sourceId", apiIngestor.getSourceId(), "status", "configured");
    }
    
    @PostMapping("/process-api")
    public CompletableFuture<ProcessingResult> processApi() {
        return processingService.processSource(apiIngestor.getSourceId());
    }
    
    @PostMapping("/process-all")
    public CompletableFuture<ProcessingResult> processAll() {
        return processingService.processAllSources();
    }
    
    @GetMapping("/data")
    public List<NormalizedRecord> getData(
            @RequestParam(required = false) String sourceId) {
        if (sourceId != null) {
            return processingService.getProcessedDataBySource(sourceId);
        }
        return processingService.getProcessedData();
    }
    
    @GetMapping("/insights")
    public List<InsightDTO> getOpenInsights() {
        return decisionService.getOpenInsights().stream()
            .map(InsightDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    @GetMapping("/insights/{recordId}")
    public List<InsightDTO> getInsightsForRecord(@PathVariable Long recordId) {
        return decisionService.getInsightsForRecord(recordId).stream()
            .map(InsightDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    @PostMapping("/insights/{insightId}/approve")
    public Map<String, String> approveInsight(
            @PathVariable Long insightId,
            @RequestBody InsightActionRequest request) {
        insightActionService.approveInsight(insightId, request.getUserId(), request.getReason());
        return Map.of("status", "approved", "insightId", insightId.toString());
    }
    
    @PostMapping("/insights/{insightId}/override")
    public Map<String, String> overrideInsight(
            @PathVariable Long insightId,
            @RequestBody InsightActionRequest request) {
        insightActionService.overrideInsight(insightId, request.getUserId(), 
            request.getOverrideData(), request.getReason());
        return Map.of("status", "overridden", "insightId", insightId.toString());
    }
    
    @PostMapping("/insights/{insightId}/archive")
    public Map<String, String> archiveInsight(
            @PathVariable Long insightId,
            @RequestBody InsightActionRequest request) {
        insightActionService.archiveInsight(insightId, request.getUserId(), request.getReason());
        return Map.of("status", "archived", "insightId", insightId.toString());
    }
    
    public static class ApiConfig {
        private String endpoint;
        private String method;
        
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }
}

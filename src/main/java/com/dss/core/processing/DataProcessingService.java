package com.dss.core.processing;

import com.dss.core.ingestion.DataIngestor;
import com.dss.core.ingestion.NormalizedRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataProcessingService {
    
    private final Map<String, DataIngestor> ingestors = new HashMap<>();
    private final List<NormalizedRecord> processedData = Collections.synchronizedList(new ArrayList<>());
    private final DecisionService decisionService;
    
    public DataProcessingService(DecisionService decisionService) {
        this.decisionService = decisionService;
    }
    
    public void registerIngestor(DataIngestor ingestor) {
        ingestors.put(ingestor.getSourceId(), ingestor);
    }
    
    public void unregisterIngestor(String sourceId) {
        ingestors.remove(sourceId);
    }
    
    public CompletableFuture<ProcessingResult> processAllSources() {
        List<CompletableFuture<List<NormalizedRecord>>> futures = ingestors.values().stream()
                .map(DataIngestor::ingest)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<NormalizedRecord> allRecords = new ArrayList<>();
                    for (CompletableFuture<List<NormalizedRecord>> future : futures) {
                        try {
                            allRecords.addAll(future.join());
                        } catch (Exception e) {
                            log.error("Error joining future", e);
                        }
                    }
                    
                    processedData.clear();
                    processedData.addAll(allRecords);
                    
                    // Process through decision service
                    try {
                        for (NormalizedRecord record : allRecords) {
                            decisionService.processRecord(record);
                        }
                    } catch (Exception e) {
                        log.error("Error processing records through decision service", e);
                    }
                    
                    return ProcessingResult.builder()
                            .totalRecords(allRecords.size())
                            .sourceCount(ingestors.size())
                            .timestamp(System.currentTimeMillis())
                            .status("completed")
                            .build();
                });
    }
    
    public CompletableFuture<ProcessingResult> processSource(String sourceId) {
        DataIngestor ingestor = ingestors.get(sourceId);
        if (ingestor == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Source not found: " + sourceId));
        }
        
        return ingestor.ingest()
                .thenApply(records -> {
                    processedData.addAll(records);
                    
                    // Process through decision service
                    try {
                        for (NormalizedRecord record : records) {
                            decisionService.processRecord(record);
                        }
                    } catch (Exception e) {
                        log.error("Error processing records through decision service", e);
                    }
                    
                    return ProcessingResult.builder()
                            .totalRecords(records.size())
                            .sourceCount(1)
                            .timestamp(System.currentTimeMillis())
                            .status("completed")
                            .build();
                });
    }
    
    public List<NormalizedRecord> getProcessedData() {
        return new ArrayList<>(processedData);
    }
    
    public List<NormalizedRecord> getProcessedDataBySource(String sourceId) {
        return processedData.stream()
                .filter(r -> r.getSourceId().equals(sourceId))
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getSourceStatus() {
        Map<String, Object> status = new HashMap<>();
        ingestors.forEach((id, ingestor) -> {
            status.put(id, Map.of(
                    "name", ingestor.getSourceName(),
                    "valid", ingestor.validateConfig()
            ));
        });
        return status;
    }
}

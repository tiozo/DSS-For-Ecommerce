package com.dss.core.ingestion.impl;

import com.dss.core.ingestion.DataIngestor;
import com.dss.core.ingestion.NormalizedRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class ApiDataIngestor implements DataIngestor {
    
    private String sourceId;
    private String apiEndpoint;
    private String apiMethod = "GET";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public ApiDataIngestor(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.sourceId = "api-" + UUID.randomUUID();
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    public void setApiEndpoint(String endpoint) {
        this.apiEndpoint = endpoint;
    }
    
    public void setApiMethod(String method) {
        this.apiMethod = method;
    }
    
    @Override
    public CompletableFuture<List<NormalizedRecord>> ingest() {
        if (!validateConfig()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        Mono<String> response = webClient.method(org.springframework.http.HttpMethod.valueOf(apiMethod))
                .uri(apiEndpoint)
                .retrieve()
                .bodyToMono(String.class);
        
        return response.map(this::parseResponse)
                .toFuture()
                .exceptionally(e -> {
                    throw new RuntimeException("API call failed: " + e.getMessage(), e);
                });
    }
    
    private List<NormalizedRecord> parseResponse(String jsonResponse) {
        List<NormalizedRecord> records = new ArrayList<>();
        try {
            Object parsed = objectMapper.readValue(jsonResponse, Object.class);
            
            if (parsed instanceof List) {
                List<?> list = (List<?>) parsed;
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> data = objectMapper.convertValue(list.get(i), Map.class);
                    records.add(NormalizedRecord.builder()
                            .sourceId(sourceId)
                            .recordId(sourceId + "-" + i)
                            .timestamp(LocalDateTime.now())
                            .data(data)
                            .status("valid")
                            .build());
                }
            } else if (parsed instanceof Map) {
                Map<String, Object> data = objectMapper.convertValue(parsed, Map.class);
                records.add(NormalizedRecord.builder()
                        .sourceId(sourceId)
                        .recordId(sourceId + "-0")
                        .timestamp(LocalDateTime.now())
                        .data(data)
                        .status("valid")
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON parsing failed", e);
        }
        
        return records;
    }
    
    @Override
    public String getSourceId() {
        return sourceId;
    }
    
    @Override
    public String getSourceName() {
        return "API Endpoint: " + apiEndpoint;
    }
    
    @Override
    public boolean validateConfig() {
        return apiEndpoint != null && !apiEndpoint.trim().isEmpty();
    }
}

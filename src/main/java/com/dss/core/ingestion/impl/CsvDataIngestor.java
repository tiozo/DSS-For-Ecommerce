package com.dss.core.ingestion.impl;

import com.dss.core.ingestion.DataIngestor;
import com.dss.core.ingestion.NormalizedRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class CsvDataIngestor implements DataIngestor {
    
    private String sourceId;
    private String csvContent;
    private boolean hasHeader = true;
    
    public CsvDataIngestor() {
        this.sourceId = "csv-upload-" + UUID.randomUUID();
    }
    
    public void setCsvContent(String content) {
        this.csvContent = content;
    }
    
    @Override
    public CompletableFuture<List<NormalizedRecord>> ingest() {
        return CompletableFuture.supplyAsync(() -> {
            if (!validateConfig()) {
                return Collections.emptyList();
            }
            
            List<NormalizedRecord> records = new ArrayList<>();
            try (StringReader reader = new StringReader(csvContent);
                 CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
                
                int recordCount = 0;
                for (CSVRecord record : parser) {
                    Map<String, Object> data = new HashMap<>(record.toMap());
                    
                    NormalizedRecord normalized = NormalizedRecord.builder()
                            .sourceId(sourceId)
                            .recordId(sourceId + "-" + recordCount)
                            .timestamp(LocalDateTime.now())
                            .data(data)
                            .status("valid")
                            .build();
                    
                    records.add(normalized);
                    recordCount++;
                }
            } catch (IOException e) {
                throw new RuntimeException("CSV parsing failed", e);
            }
            
            return records;
        });
    }
    
    @Override
    public String getSourceId() {
        return sourceId;
    }
    
    @Override
    public String getSourceName() {
        return "CSV Upload";
    }
    
    @Override
    public boolean validateConfig() {
        return csvContent != null && !csvContent.trim().isEmpty();
    }
}

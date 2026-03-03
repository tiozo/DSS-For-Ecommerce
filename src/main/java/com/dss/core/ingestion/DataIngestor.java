package com.dss.core.ingestion;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generic interface for data ingestion from various sources.
 * Implementations handle CSV, API polling, webhooks, etc.
 */
public interface DataIngestor {
    
    /**
     * Ingest data and return normalized records.
     * @return CompletableFuture for async processing
     */
    CompletableFuture<List<NormalizedRecord>> ingest();
    
    /**
     * Get the source identifier (e.g., "csv-upload", "api-endpoint-1")
     */
    String getSourceId();
    
    /**
     * Get human-readable source name
     */
    String getSourceName();
    
    /**
     * Validate configuration before ingestion
     */
    boolean validateConfig();
}

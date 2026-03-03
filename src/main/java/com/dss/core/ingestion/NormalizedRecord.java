package com.dss.core.ingestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Unified data format for all ingested records.
 * Ensures consistency regardless of source (CSV, API, webhook).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedRecord {
    private String sourceId;
    private String recordId;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private String status; // "valid", "warning", "error"
    private String metadata; // JSON string for additional context
}

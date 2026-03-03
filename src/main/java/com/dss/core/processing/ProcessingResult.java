package com.dss.core.processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {
    private int totalRecords;
    private int sourceCount;
    private long timestamp;
    private String status;
}

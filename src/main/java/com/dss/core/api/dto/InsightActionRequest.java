package com.dss.core.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightActionRequest {
    private String userId;
    private String reason;
    private String overrideData; // For override action only
}

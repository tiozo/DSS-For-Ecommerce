package com.dss.core.api; 

public record InsightActionRequest(
    String reason,
    String userId
) {}
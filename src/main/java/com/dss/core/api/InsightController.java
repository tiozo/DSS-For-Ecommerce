package com.dss.core.api;

import com.dss.core.persistence.entity.DecisionInsightEntity;
import com.dss.core.persistence.repository.DecisionInsightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {
    
    private final DecisionInsightRepository insightRepository;
    
    @GetMapping("/all")
    public ResponseEntity<List<DecisionInsightEntity>> getAllInsights() {
        return ResponseEntity.ok(insightRepository.findAll());
    }
}

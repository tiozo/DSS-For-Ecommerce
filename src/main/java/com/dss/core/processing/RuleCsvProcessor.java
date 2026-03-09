package com.dss.core.processing;

import com.dss.core.persistence.entity.RuleDefinitionEntity;
import com.dss.core.persistence.repository.RuleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RuleCsvProcessor {
    
    private final RuleDefinitionRepository ruleRepository;
    
    public int processCsv(MultipartFile file) throws Exception {
        List<RuleDefinitionEntity> rules = new ArrayList<>();
        
        try (var reader = new InputStreamReader(file.getInputStream())) {
            CSVParser csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            
            for (CSVRecord row : csv) {
                rules.add(RuleDefinitionEntity.builder()
                    .ruleName(row.get("ruleName"))
                    .ruleType(row.get("ruleType"))
                    .condition(row.get("condition"))
                    .threshold(row.get("threshold"))
                    .action(row.get("action"))
                    .active(true)
                    .build());
            }
        }
        
        ruleRepository.saveAll(rules);
        return rules.size();
    }
}

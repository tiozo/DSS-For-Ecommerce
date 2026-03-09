package com.dss.core.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rule_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinitionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String ruleName;
    
    @Column(name = "severity")
    private String severity;

    @Column(nullable = false)
    private String ruleType;
    
    @Column(columnDefinition = "text")
    private String condition;
    
    private String threshold;
    private String action;
    
    @Builder.Default
    private Boolean active = true;
}

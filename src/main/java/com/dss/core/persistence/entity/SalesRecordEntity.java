package com.dss.core.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesRecordEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer orderNumber;
    
    private Integer quantityOrdered;
    private BigDecimal priceEach;
    private Integer orderLineNumber;
    private BigDecimal sales;
    
    @Column(nullable = false)
    private LocalDate orderDate;
    
    private String status;
    private Integer qtrId;
    private Integer monthId;
    private Integer yearId;
    private String productLine;
    private BigDecimal msrp;
    private String productCode;
    private String customerName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String territory;
    private String contactLastName;
    private String contactFirstName;
    private String dealSize;
}

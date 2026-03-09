package com.dss.core.processing;

import com.dss.core.persistence.entity.SalesRecordEntity;
import com.dss.core.persistence.repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesCsvProcessor {
    
    private final SalesRecordRepository salesRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
    
    public int processCsv(MultipartFile file) throws Exception {
        List<SalesRecordEntity> records = new ArrayList<>();
        
        try (var reader = new InputStreamReader(file.getInputStream())) {
            CSVParser csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            
            for (CSVRecord row : csv) {
                records.add(SalesRecordEntity.builder()
                    .orderNumber(parseInt(row.get("ORDERNUMBER")))
                    .quantityOrdered(parseInt(row.get("QUANTITYORDERED")))
                    .priceEach(parseBigDecimal(row.get("PRICEEACH")))
                    .orderLineNumber(parseInt(row.get("ORDERLINENUMBER")))
                    .sales(parseBigDecimal(row.get("SALES")))
                    .orderDate(parseDate(row.get("ORDERDATE")))
                    .status(row.get("STATUS"))
                    .qtrId(parseInt(row.get("QTR_ID")))
                    .monthId(parseInt(row.get("MONTH_ID")))
                    .yearId(parseInt(row.get("YEAR_ID")))
                    .productLine(row.get("PRODUCTLINE"))
                    .msrp(parseBigDecimal(row.get("MSRP")))
                    .productCode(row.get("PRODUCTCODE"))
                    .customerName(row.get("CUSTOMERNAME"))
                    .phone(row.get("PHONE"))
                    .addressLine1(row.get("ADDRESSLINE1"))
                    .addressLine2(row.get("ADDRESSLINE2"))
                    .city(row.get("CITY"))
                    .state(row.get("STATE"))
                    .postalCode(row.get("POSTALCODE"))
                    .country(row.get("COUNTRY"))
                    .territory(row.get("TERRITORY"))
                    .contactLastName(row.get("CONTACTLASTNAME"))
                    .contactFirstName(row.get("CONTACTFIRSTNAME"))
                    .dealSize(row.get("DEALSIZE"))
                    .build());
            }
        }
        
        salesRepository.saveAll(records);
        return records.size();
    }
    
    private Integer parseInt(String value) {
        try { return value != null && !value.isEmpty() ? Integer.parseInt(value) : null; }
        catch (Exception e) { return null; }
    }
    
    private BigDecimal parseBigDecimal(String value) {
        try { return value != null && !value.isEmpty() ? new BigDecimal(value) : null; }
        catch (Exception e) { return null; }
    }
    
    private LocalDate parseDate(String value) {
        try { return LocalDate.parse(value, DATE_FORMATTER); }
        catch (Exception e) { return null; }
    }
}

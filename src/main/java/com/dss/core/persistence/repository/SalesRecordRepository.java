package com.dss.core.persistence.repository;

import com.dss.core.persistence.entity.SalesRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecordEntity, Long> {
    
    @Query("SELECT FUNCTION('TO_CHAR', s.orderDate, 'YYYY-MM') as month, SUM(s.sales) as total " +
           "FROM SalesRecordEntity s GROUP BY FUNCTION('TO_CHAR', s.orderDate, 'YYYY-MM') ORDER BY month")
    List<Object[]> getSalesByMonth();
    
    @Query("SELECT s.productLine, SUM(s.sales) FROM SalesRecordEntity s GROUP BY s.productLine")
    List<Object[]> getSalesByProductLine();
    
    @Query("SELECT s.status, COUNT(s) FROM SalesRecordEntity s GROUP BY s.status")
    List<Object[]> getStatusDistribution();
}

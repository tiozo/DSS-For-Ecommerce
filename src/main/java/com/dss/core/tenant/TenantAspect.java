package com.dss.core.tenant;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.Optional;
import com.dss.core.persistence.entity.DecisionInsightEntity;

@Aspect
@Component
@Slf4j
public class TenantAspect {
    
    @Around("execution(* com.dss.core.persistence.repository.DecisionInsightRepository.findById(..))")
    public Object injectTenantId(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed(); 
        
        if (result instanceof Optional<?> optional && optional.isPresent()) {
            Object entity = optional.get();
            
            if (entity instanceof DecisionInsightEntity insight) {
                String currentTenant = TenantContext.getTenantId();
                
                if (!insight.getTenantId().equals(currentTenant)) {
                    log.warn("Security Alert: Tenant {} tried to access Insight of Tenant {}", 
                            currentTenant, insight.getTenantId());
                    return Optional.empty(); 
                }
            }
        }
        
        return result;
    }
}

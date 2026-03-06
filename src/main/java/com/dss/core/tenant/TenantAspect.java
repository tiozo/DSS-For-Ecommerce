package com.dss.core.tenant;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * TenantAspect: Automatically injects tenant_id into repository method calls
 * Ensures all queries are scoped to the current tenant
 */
@Aspect
@Component
@Slf4j
public class TenantAspect {
    
    @Around("execution(* com.dss.core.persistence.repository.*.find*(..))")
    public Object injectTenantId(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        
        // If first argument is not already a tenant ID, inject it
        if (args.length > 0 && !(args[0] instanceof String && args[0].equals(TenantContext.getTenantId()))) {
            Object[] newArgs = new Object[args.length + 1];
            newArgs[0] = TenantContext.getTenantId();
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return joinPoint.proceed(newArgs);
        }
        
        return joinPoint.proceed(args);
    }
}

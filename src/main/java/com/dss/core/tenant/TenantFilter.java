package com.dss.core.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TenantFilter: Extracts tenant_id from request headers and sets TenantContext
 */
@Component
@Slf4j
public class TenantFilter implements Filter {
    
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String tenantId = httpRequest.getHeader(TENANT_HEADER);
            
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setTenantId(tenantId);
                log.debug("Tenant context set: {}", tenantId);
            }
            
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

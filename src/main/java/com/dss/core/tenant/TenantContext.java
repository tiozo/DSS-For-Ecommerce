package com.dss.core.tenant;

/**
 * TenantContext: Thread-safe tenant isolation using ThreadLocal
 * Ensures every database operation is scoped to the current tenant
 */
public class TenantContext {
    
    private static final ThreadLocal<String> tenantId = new ThreadLocal<>();
    
    public static void setTenantId(String id) {
        tenantId.set(id);
    }
    
    public static String getTenantId() {
        String id = tenantId.get();
        return id != null ? id : "default";
    }
    
    public static void clear() {
        tenantId.remove();
    }
}

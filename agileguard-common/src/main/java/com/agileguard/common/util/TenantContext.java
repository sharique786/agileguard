package com.agileguard.common.util;

/**
 * Thread-local holder for the current tenant ID.
 * Set by the JWT filter on every request and used in repositories
 * to enforce tenant data isolation in all database queries.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /** Sets the tenant ID for the current request thread. */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /** Returns the tenant ID for the current request thread. */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /** Clears the tenant ID at the end of a request (call in filter finally block). */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

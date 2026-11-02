package org.smart.erp.common.security;

public interface CurrentUser {

    default String getTenantId() { return org.smart.erp.tenant.context.TenantScope.required(); }

    Long getUserId();

    String getUsername();
}

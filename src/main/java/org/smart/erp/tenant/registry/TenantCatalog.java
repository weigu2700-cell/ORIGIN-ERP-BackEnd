package org.smart.erp.tenant.registry;

public interface TenantCatalog {
    TenantDescriptor byId(String id);
    TenantDescriptor byCode(String code);
}

package org.smart.erp.tenant.registry;

/** 企业登记目录，支持按企业 ID 或编码查询。 */
public interface TenantCatalog {

    TenantDescriptor byId(String id);

    TenantDescriptor byCode(String code);
}

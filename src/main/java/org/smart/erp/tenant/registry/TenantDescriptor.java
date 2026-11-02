package org.smart.erp.tenant.registry;

import org.smart.erp.tenant.TenantException;

public record TenantDescriptor(String id, String code, String name, String connectionRef,
                               long routingVersion, String status, String schemaStatus) {
    public TenantDescriptor requireActive() {
        if (!"ACTIVE".equals(status)) throw new TenantException(403, "企业尚未开通或已停用");
        if (!"READY".equals(schemaStatus)) throw new TenantException(503, "企业数据库尚未就绪");
        return this;
    }
}

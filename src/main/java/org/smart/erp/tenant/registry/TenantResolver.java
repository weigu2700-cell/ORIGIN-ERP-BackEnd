package org.smart.erp.tenant.registry;

import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.config.TenantProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {
    private final TenantProperties properties;
    private final ObjectProvider<TenantCatalog> catalog;
    public TenantResolver(TenantProperties properties, ObjectProvider<TenantCatalog> catalog) {
        this.properties = properties; this.catalog = catalog;
    }
    public TenantDescriptor login(String code) {
        if (!properties.multi()) {
            if (code != null && !code.isBlank() && !properties.getSingleTenantCode().equals(code.trim()))
                throw new TenantException(403, "企业编码不匹配");
            return single();
        }
        if (code == null || code.isBlank()) throw new TenantException(400, "请输入企业编码");
        return catalog.getObject().byCode(code.trim());
    }
    public TenantDescriptor authenticated(String id) {
        if (id == null || id.isBlank()) {
            if (properties.multi() || !properties.isAllowLegacyTokens()) throw new TenantException(401, "登录身份缺少企业信息，请重新登录");
            return single();
        }
        if (properties.multi()) return catalog.getObject().byId(id);
        if (!properties.getSingleTenantId().equals(id)) throw new TenantException(403, "登录身份不属于当前企业");
        return single();
    }
    private TenantDescriptor single() {
        return new TenantDescriptor(properties.getSingleTenantId(), properties.getSingleTenantCode(), "当前企业", "", 1, "ACTIVE", "READY", org.smart.erp.tenant.migration.TenantFlyway.TENANT_VERSION);
    }
}

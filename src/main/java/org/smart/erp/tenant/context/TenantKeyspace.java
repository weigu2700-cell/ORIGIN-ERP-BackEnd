package org.smart.erp.tenant.context;

import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.config.TenantProperties;
import org.springframework.stereotype.Component;

/** single 保留既有 Redis 序列/缓存；multi 缺少企业身份时绝不回退旧全局键。 */
@Component
public final class TenantKeyspace {
    private final TenantProperties properties;
    public TenantKeyspace(TenantProperties properties) { this.properties = properties; }
    public boolean multi() { return properties.multi(); }
    public String tenantId() {
        if (multi()) return TenantScope.required();
        String current = TenantScope.current();
        if (current != null && !current.equals(properties.getSingleTenantId()))
            throw new TenantException(403, "企业上下文与单企业配置不匹配");
        return properties.getSingleTenantId();
    }
    public String key(String raw) {
        if (raw == null) throw new IllegalArgumentException("Redis key 不能为空");
        String tenant = tenantId();
        return multi() ? "erp:t:" + tenant + ":" + raw : raw;
    }
    public String unmap(String physical) {
        if (!multi() || physical == null) return physical;
        String prefix = "erp:t:" + tenantId() + ":";
        if (!physical.startsWith(prefix)) throw new TenantException(403, "拒绝读取其他企业的 Redis key");
        return physical.substring(prefix.length());
    }
}

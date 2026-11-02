package org.smart.erp.tenant.context;

import org.smart.erp.tenant.registry.TenantResolver;
import java.util.function.Supplier;

/** 可在线程间传递的企业快照；每个执行边界重新检查企业状态并恢复原线程上下文。 */
public final class TenantWork {
    private final String tenantId;
    private final TenantResolver resolver;
    TenantWork(String tenantId, TenantResolver resolver) { this.tenantId = tenantId; this.resolver = resolver; }
    public String tenantId() { return tenantId; }
    public <T> T call(Supplier<T> action) {
        resolver.authenticated(tenantId);
        try (var scope = TenantScope.open(tenantId)) { return action.get(); }
    }
    public void run(Runnable action) { call(() -> { action.run(); return null; }); }
}

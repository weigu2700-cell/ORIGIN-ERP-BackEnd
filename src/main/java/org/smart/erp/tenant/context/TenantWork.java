package org.smart.erp.tenant.context;

import org.smart.erp.tenant.registry.TenantResolver;

import java.util.function.Supplier;

/**
 * 可在线程间传递的企业快照。
 *
 * <p>每个执行边界都会重新校验企业状态，并在执行结束后恢复原线程的租户作用域。</p>
 */
public final class TenantWork {

    private final String tenantId;
    private final TenantResolver resolver;

    TenantWork(String tenantId, TenantResolver resolver) {
        this.tenantId = tenantId;
        this.resolver = resolver;
    }

    public String tenantId() {
        return tenantId;
    }

    public <T> T call(Supplier<T> action) {
        resolver.authenticated(tenantId);
        try (TenantScope scope = TenantScope.open(tenantId)) {
            return action.get();
        }
    }

    public void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }
}

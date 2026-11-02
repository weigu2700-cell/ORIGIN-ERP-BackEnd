package org.smart.erp.tenant.context;

import org.smart.erp.tenant.TenantException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 当前线程的租户作用域。
 *
 * <p>只允许同一租户的嵌套作用域；线程池不会隐式继承，必须显式传播。</p>
 */
public final class TenantScope implements AutoCloseable {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private final String previous;
    private final Thread owner = Thread.currentThread();
    private boolean closed;

    private TenantScope(String tenantId) {
        if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new TenantException(403, "缺少有效租户身份");
        }

        previous = CURRENT.get();
        if (previous != null && !previous.equals(tenantId)) {
            throw new TenantException(403, "禁止在租户作用域内切换企业");
        }
        if (previous == null && TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new TenantException(403, "租户身份必须在事务开始前确定");
        }
        CURRENT.set(tenantId);
    }

    public static TenantScope open(String tenantId) {
        return new TenantScope(tenantId);
    }

    public static String current() {
        return CURRENT.get();
    }

    public static String required() {
        String tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new TenantException(403, "缺少租户上下文，拒绝访问数据库");
        }
        return tenantId;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("租户作用域必须在原线程关闭");
        }

        closed = true;
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}

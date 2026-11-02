package org.smart.erp.tenant.context;

import org.smart.erp.common.security.LoginUser;
import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.registry.TenantResolver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class TenantExecutor {
    private final TenantKeyspace keys;
    private final TenantResolver resolver;
    public TenantExecutor(TenantKeyspace keys, TenantResolver resolver) { this.keys = keys; this.resolver = resolver; }
    public TenantWork forTenant(String tenantId) {
        if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,64}")) throw new TenantException(403, "缺少有效企业身份");
        resolver.authenticated(tenantId);
        return new TenantWork(tenantId, resolver);
    }
    public TenantWork captureTenant() { return forTenant(keys.tenantId()); }
    public TenantExecution capture() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser user) || user.getUserId() == null)
            throw new TenantException(401, "缺少有效用户身份");
        String id = keys.tenantId();
        if (!id.equals(user.getTenantId()) && (keys.multi() || user.getTenantId() != null))
            throw new TenantException(403, "用户与企业身份不匹配");
        return new TenantExecution(forTenant(id), user, auth);
    }
}

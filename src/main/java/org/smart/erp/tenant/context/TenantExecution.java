package org.smart.erp.tenant.context;

import org.smart.erp.common.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.function.Supplier;

/** 不持有可变的请求 SecurityContext 或 LoginUser，避免异步期间身份被切换。 */
public final class TenantExecution {
    public static final String CONTEXT_KEY = "tenantExecution";
    private final TenantWork work;
    private final Long userId;
    private final String username;
    private final java.util.List<String> authorities;
    TenantExecution(TenantWork work, LoginUser user, Authentication authentication) {
        this.work = work; this.userId = user.getUserId(); this.username = user.getUsername();
        this.authorities = authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    }
    public String tenantId() { return work.tenantId(); }
    public Long userId() { return userId; }
    public <T> T call(Supplier<T> action) {
        return work.call(() -> {
            var previous = SecurityContextHolder.getContext();
            var current = SecurityContextHolder.createEmptyContext();
            current.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    new LoginUser(userId, username, tenantId()), null,
                    authorities.stream().map(SimpleGrantedAuthority::new).toList()));
            SecurityContextHolder.setContext(current);
            try { return action.get(); }
            finally { SecurityContextHolder.setContext(previous); }
        });
    }
    public void run(Runnable action) { call(() -> { action.run(); return null; }); }
}

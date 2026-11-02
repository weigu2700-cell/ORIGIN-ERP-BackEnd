package org.smart.erp.tenant.context;

import org.smart.erp.common.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

/**
 * 可在线程间传递的租户与用户快照。
 *
 * <p>只保存不可变字段，不持有请求级 {@link SecurityContext} 或 {@link LoginUser}，
 * 避免异步执行期间身份被其他请求切换。</p>
 */
public final class TenantExecution {

    public static final String CONTEXT_KEY = "tenantExecution";

    private final TenantWork work;
    private final Long userId;
    private final String username;
    private final List<String> authorities;

    TenantExecution(TenantWork work, LoginUser user, Authentication authentication) {
        this.work = work;
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public String tenantId() {
        return work.tenantId();
    }

    public Long userId() {
        return userId;
    }

    public <T> T call(Supplier<T> action) {
        return work.call(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContext current = SecurityContextHolder.createEmptyContext();
            current.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    new LoginUser(userId, username, tenantId()),
                    null,
                    authorities.stream().map(SimpleGrantedAuthority::new).toList()
            ));
            SecurityContextHolder.setContext(current);
            try {
                return action.get();
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        });
    }

    public void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }
}

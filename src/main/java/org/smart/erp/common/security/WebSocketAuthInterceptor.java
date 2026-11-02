package org.smart.erp.common.security;

import org.jspecify.annotations.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import org.smart.erp.tenant.registry.TenantResolver;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.TenantException;
import org.smart.erp.system.service.UserService;
import org.smart.erp.system.Enum.UserStatus;
import org.springframework.http.HttpStatus;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final TenantResolver tenants;
    private final UserService users;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, TenantResolver tenants, UserService users) {
        this.jwtUtil = jwtUtil;
        this.tenants = tenants;
        this.users = users;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null) { response.setStatusCode(HttpStatus.UNAUTHORIZED); return false; }
        try {
            var identity = jwtUtil.parseIdentity(token);
            var tenant = tenants.authenticated(identity.tenantId());
            String requested = request.getHeaders().getFirst("X-Tenant-ID");
            if (requested != null && !requested.equals(tenant.id())) throw new TenantException(403, "企业身份冲突");
            try (var scope = TenantScope.open(tenant.id())) {
                var user = users.detailUserById(identity.userId());
                if (user == null || user.getStatus() != UserStatus.NORMAL) throw new TenantException(403, "账号不可用");
            }
            attributes.put("tenantId", tenant.id());
            attributes.put("userId", identity.userId());
            attributes.put("expiresAt", identity.expiresAt());
            return true;
        } catch (TenantException error) {
            response.setStatusCode(HttpStatus.valueOf(error.status()));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException invalid) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
        } catch (org.smart.erp.common.exception.BusinessException missing) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
        } catch (RuntimeException unavailable) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {}
}

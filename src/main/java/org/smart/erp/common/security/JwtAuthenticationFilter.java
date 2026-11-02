package org.smart.erp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.service.UserService;
import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.registry.TenantResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final UserService users;
    private final PermissionService permissions;
    private final TenantResolver tenants;
    private final TenantProperties properties;
    private final ObjectMapper mapper;
    public JwtAuthenticationFilter(JwtUtil jwt, UserService users, PermissionService permissions,
                                   TenantResolver tenants, TenantProperties properties, ObjectMapper mapper) {
        this.jwt = jwt; this.users = users; this.permissions = permissions;
        this.tenants = tenants; this.properties = properties; this.mapper = mapper;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean login = path.equals("/auth/login") || path.equals("/auth/tenant/resolve");
        if (properties.multi()) {
            // T2 未完成缓存/通知/AI 隔离，明确禁止现有业务接口进入多租户链路。
            if (!login && !path.startsWith("/tenant-verification/") && !path.equals("/error")) {
                reject(response, 503, "多租户业务入口尚未开放，请先完成全链路隔离验证"); return;
            }
        }
        // 登录使用本次账号密码，不能被客户端残留的过期 Bearer 阻止。
        if (login) { chain.doFilter(request, response); return; }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            if (properties.multi()) reject(response, 401, "未认证或登录已失效");
            else chain.doFilter(request, response);
            return;
        }
        String tenantId;
        long userId;
        try {
            var identity = jwt.parseIdentity(authorization.substring(7));
            tenantId = tenants.authenticated(identity.tenantId()).id();
            userId = identity.userId();
            String requested = request.getHeader("X-Tenant-ID");
            if (requested != null && !requested.equals(tenantId)) throw new TenantException(403, "请求企业与登录身份不匹配");
        } catch (TenantException error) { reject(response, error.status(), error.getMessage()); return;
        } catch (JwtException | IllegalArgumentException error) { reject(response, 401, "未认证或登录已失效"); return; }

        try (var scope = TenantScope.open(tenantId)) {
            try {
                var user = users.detailUserById(userId);
                if (user.getStatus() != UserStatus.NORMAL) throw new TenantException(403, "账号不可用");
                // 验证模式不读取尚未按租户隔离的权限缓存。
                var authorities = properties.multi() ? List.<SimpleGrantedAuthority>of()
                        : permissions.detailCurrentUserPermission(userId).stream()
                            .map(permission -> new SimpleGrantedAuthority(permission.getCode())).toList();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        new LoginUser(userId, user.getUsername(), tenantId), null, authorities));
            } catch (TenantException error) { reject(response, error.status(), error.getMessage()); return;
            } catch (DataAccessException error) { reject(response, 503, "企业数据库暂时不可用"); return;
            } catch (BusinessException error) { reject(response, 401, "账号不存在或登录已失效"); return; }
            chain.doFilter(request, response);
        } finally { SecurityContextHolder.clearContext(); }
    }
    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status); response.setCharacterEncoding("UTF-8"); response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), Result.fail(status, message));
    }
}

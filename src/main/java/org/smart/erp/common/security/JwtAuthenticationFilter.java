package org.smart.erp.common.security;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.service.UserService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

    @Resource
    private PermissionService permissionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // 没有 Bearer 头 -> 匿名访问，直接放行（避免 NPE）
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        try {
            long userId = jwtUtil.parseToken(token);
            User user = userService.getUserById(userId);

            LoginUser loginUser = new LoginUser(
                    user.getId(),
                    user.getUsername()
            );
            List<String> permissions = permissionService.getCurrentUserPermissionById(user.getId())
                    .stream()
                    .map(org.smart.erp.system.vo.PermissionVO::getCode)
                    .toList();
            List<SimpleGrantedAuthority> authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            loginUser,
                            null,
                            authorities
                    );
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

}

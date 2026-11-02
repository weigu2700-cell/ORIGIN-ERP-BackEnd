package org.smart.erp.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.smart.erp.tenant.context.TenantExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** 单实例通知：显式按企业与用户分组，不把 WebSocket 当可靠消息队列。 */
@Slf4j
@Component
public class WebSocketSessionManager {
    private record Recipient(String tenantId, Long userId) {}
    private final ConcurrentHashMap<Recipient, ConcurrentHashMap<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final TenantExecutor tenants;
    public WebSocketSessionManager(TenantExecutor tenants) { this.tenants = tenants; }

    public void addSession(String tenantId, Long userId, WebSocketSession session) {
        if (userId == null || session == null) throw new IllegalArgumentException("缺少 WebSocket 用户身份");
        tenants.forTenant(tenantId);
        sessions.compute(new Recipient(tenantId, userId), (key, existing) -> {
            var group = existing == null ? new ConcurrentHashMap<String, WebSocketSession>() : existing;
            group.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 10000, 65536));
            return group;
        });
    }
    public void removeSession(String tenantId, Long userId, WebSocketSession session) {
        if (tenantId == null || userId == null || session == null) return;
        sessions.computeIfPresent(new Recipient(tenantId, userId), (key, group) -> {
            group.remove(session.getId()); return group.isEmpty() ? null : group;
        });
    }
    public void sendMessage(String tenantId, Long userId, String message) {
        var key = new Recipient(tenantId, userId);
        if (!sessions.containsKey(key)) return;
        if (!active(key)) return;
        for (var session : sessions.getOrDefault(key, new ConcurrentHashMap<>()).values()) {
            if (!usable(key, session)) continue;
            try { session.sendMessage(new TextMessage(message)); }
            catch (IOException | RuntimeException error) {
                log.warn("WebSocket 推送失败 tenantId={},userId={}", tenantId, userId);
                close(key, session);
            }
        }
    }
    // 定期失效空闲连接，停用企业无需等到下一次推送才断开。
    @Scheduled(fixedDelayString="${tenancy.websocket-check-interval-ms:10000}")
    public void reap() {
        for (var key : sessions.keySet()) {
            if (active(key)) for (var session : sessions.getOrDefault(key, new ConcurrentHashMap<>()).values()) usable(key, session);
        }
    }
    private boolean active(Recipient key) {
        try { tenants.forTenant(key.tenantId()); return true; }
        catch (RuntimeException unavailable) {
            for (var session : sessions.getOrDefault(key, new ConcurrentHashMap<>()).values()) close(key, session);
            return false;
        }
    }
    private boolean usable(Recipient key, WebSocketSession session) {
        Object expiry = session.getAttributes().get("expiresAt");
        if (!session.isOpen() || !(expiry instanceof Instant expires) || !expires.isAfter(Instant.now())) {
            close(key, session); return false;
        }
        return true;
    }
    private void close(Recipient key, WebSocketSession session) {
        removeSession(key.tenantId(), key.userId(), session);
        try { if (session.isOpen()) session.close(CloseStatus.POLICY_VIOLATION); }
        catch (IOException | RuntimeException ignored) { /* 失效连接已经从索引移除。 */ }
    }
}

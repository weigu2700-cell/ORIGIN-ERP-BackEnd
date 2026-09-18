package org.smart.erp.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        if (userId == null || session == null) return;
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.<WebSocketSession>newKeySet())
                .add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        if (userId == null) return;
        sessions.computeIfPresent(userId, (k, set) -> {
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
    }

    public void sendMessage(Long userId, String message) {
        if (userId == null) return;

        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) return;

        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : userSessions) {
            if (session == null || !session.isOpen()) continue;
            try {
                session.sendMessage(textMessage);
            } catch (IOException e) {
                log.warn("WebSocket 推送失败，移除失效会话 userId={}", userId, e);
                removeSession(userId, session);
            }
        }
    }
}

package org.smart.erp.common.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public void removeSession(Long userId) {
        sessions.remove(userId);
    }

    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }
}

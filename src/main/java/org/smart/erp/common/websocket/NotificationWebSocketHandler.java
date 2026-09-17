package org.smart.erp.common.websocket;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager webSocketSessionManager;

    public NotificationWebSocketHandler(
            WebSocketSessionManager webSocketSessionManager
    ) {
        this.webSocketSessionManager = webSocketSessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId =
                (Long) session.getAttributes().get("userId");

        webSocketSessionManager.addSession(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        Long userId =
                (Long) session.getAttributes().get("userId");
        webSocketSessionManager.removeSession(userId);
    }
}

package org.smart.erp.common.config;

import org.smart.erp.common.security.WebSocketAuthInterceptor;
import org.smart.erp.common.websocket.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(
            NotificationWebSocketHandler handler,
            WebSocketAuthInterceptor authInterceptor
    ) {
        this.handler = handler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notification")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}

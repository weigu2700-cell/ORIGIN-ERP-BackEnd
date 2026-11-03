package org.smart.erp.im;

import org.smart.erp.im.websocket.ImHandshake;
import org.smart.erp.im.websocket.ImSockets;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(ImProperties.class)
public class ImConfiguration implements WebSocketConfigurer {

    private final ImSockets sockets;

    private final ImHandshake handshake;

    private final ImProperties properties;

    public ImConfiguration(ImSockets sockets, ImHandshake handshake, ImProperties properties) {
        this.sockets = sockets;
        this.handshake = handshake;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Origin 由握手拦截器精确匹配；原生无 Origin 请求仍需一次性票据。
        registry.addHandler(sockets, "/ws/chat")
            .addInterceptors(handshake)
            .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
    }

    @Bean("imPushExecutor")
    public ThreadPoolTaskExecutor imPushExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("im-push-");
        return executor;
    }

    @Bean
    @ConditionalOnProperty(name = "im.enabled", havingValue = "true")
    public RedisMessageListenerContainer imWakeupListener(RedisConnectionFactory factory, ImJson json,
            @org.springframework.beans.factory.annotation.Qualifier("imPushExecutor") ThreadPoolTaskExecutor executor) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) -> {
            try {
                var notice = json.read(new String(message.getBody(), StandardCharsets.UTF_8), Wakeup.class);
                executor.execute(() -> sockets.wake(notice.tenantId(), notice.userId()));
            }
            catch (RuntimeException error) {
                // 唤醒丢失不删除持久消息；客户端的周期补拉负责收敛。
                LoggerFactory.getLogger(ImConfiguration.class)
                    .warn("IM wakeup unavailable: {}", error.getClass().getSimpleName());
            }
        }, new ChannelTopic(ImRedis.CHANNEL));
        return container;
    }

    public record Wakeup(String tenantId, long userId) {
    }

}

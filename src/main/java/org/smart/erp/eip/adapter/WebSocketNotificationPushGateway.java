package org.smart.erp.eip.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.common.websocket.WebSocketSessionManager;
import org.smart.erp.eip.entity.Notification;
import org.smart.erp.eip.port.NotificationPushGateway;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 只适配既有会话管理器，不复制 WebSocket endpoint、handler 或会话状态。 */
@Slf4j
@Component
public class WebSocketNotificationPushGateway implements NotificationPushGateway {

	private final WebSocketSessionManager sessionManager;

	private final ObjectMapper objectMapper;

	public WebSocketNotificationPushGateway(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
		this.sessionManager = sessionManager;
		this.objectMapper = objectMapper;
	}

	@Override
	public void push(Notification notification) {
		try {
			sessionManager.sendMessage(notification.getUserId(), objectMapper.writeValueAsString(notification));
		}
		catch (JsonProcessingException | RuntimeException exception) {
			log.warn("通知已保存，WebSocket 推送消息序列化失败，notificationId={}", notification.getId(), exception);
		}
		catch (IOException exception) {
			log.warn("通知已保存，WebSocket 推送失败，notificationId={}", notification.getId(), exception);
		}
	}

}
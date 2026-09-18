package org.smart.erp.eip.event;

import lombok.Getter;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/** 原业务事务中发布，监听器在事务提交后才执行通知落库。 */
@Getter
public final class NotificationPublishEvent extends ApplicationEvent {

	private final NotificationPublishDTO publish;

	private final String requestId;

	public NotificationPublishEvent(Object source, NotificationPublishDTO publish) {
		super(source);
		this.publish = publish;
		this.requestId = publish.getRequestId() == null || publish.getRequestId().isBlank()
				? UUID.randomUUID().toString() : publish.getRequestId();
	}

}
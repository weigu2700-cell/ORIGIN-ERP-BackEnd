package org.smart.erp.eip.service.impl;

import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.event.NotificationPublishEvent;
import org.smart.erp.eip.service.NotificationPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.smart.erp.tenant.context.TenantExecutor;

/** 发布器只产生领域事件，不感知收件人目录、数据库或推送实现。 */
@Service
public class NotificationPublisherImpl implements NotificationPublisher {

	private final ApplicationEventPublisher eventPublisher;
	private final TenantExecutor tenants;

	public NotificationPublisherImpl(ApplicationEventPublisher eventPublisher, TenantExecutor tenants) {
		this.eventPublisher = eventPublisher;
		this.tenants = tenants;
	}

	@Override
	public void publish(NotificationPublishDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("通知发布参数不能为空");
		}
		eventPublisher.publishEvent(new NotificationPublishEvent(this, dto, tenants.captureTenant().tenantId()));
	}

}

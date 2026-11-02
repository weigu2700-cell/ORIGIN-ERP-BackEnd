package org.smart.erp.eip.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.eip.converter.NotificationConverter;
import org.smart.erp.eip.entity.Notification;
import org.smart.erp.eip.entity.NotificationPublish;
import org.smart.erp.eip.event.NotificationPublishEvent;
import org.smart.erp.eip.mapper.NotificationMapper;
import org.smart.erp.eip.mapper.NotificationPublishMapper;
import org.smart.erp.eip.port.NotificationPushGateway;
import org.smart.erp.eip.service.NotificationPersistenceService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.smart.erp.tenant.context.TenantExecutor;
import org.smart.erp.tenant.context.TenantWork;

/** 独立事务写入一条发布批次和该批次的全部收件箱记录。 */
@Slf4j
@Service
public class NotificationPersistenceServiceImpl implements NotificationPersistenceService {

	private final NotificationPublishMapper publishMapper;

	private final NotificationMapper notificationMapper;

	private final NotificationConverter converter;

	private final NotificationPushGateway pushGateway;
	private final TenantExecutor tenants;

	public NotificationPersistenceServiceImpl(NotificationPublishMapper publishMapper,
			NotificationMapper notificationMapper, NotificationConverter converter,
			NotificationPushGateway pushGateway, TenantExecutor tenants) {
		this.publishMapper = publishMapper;
		this.notificationMapper = notificationMapper;
		this.converter = converter;
		this.pushGateway = pushGateway;
		this.tenants = tenants;
	}

	@Override
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void persist(NotificationPublishEvent event, Set<Long> recipientIds) {
		if (!event.getTenantId().equals(org.smart.erp.tenant.context.TenantScope.required()))
			throw new org.smart.erp.tenant.TenantException(403, "通知事件与当前企业不匹配");
		if (recipientIds == null || recipientIds.isEmpty()) {
			throw new BusinessException(422, "未解析到有效通知收件人");
		}

		NotificationPublish publish = converter.toPublish(event.getPublish(), event.getRequestId());
		Set<Long> distinctRecipientIds = new LinkedHashSet<>(recipientIds);
		distinctRecipientIds.remove(null);
		publish.setRecipientCount(distinctRecipientIds.size());
		try {
			publishMapper.insert(publish);
		}
		catch (DuplicateKeyException duplicate) {
			log.debug("通知发布批次已处理，跳过重复发布，requestId={}", event.getRequestId());
			return;
		}

		List<Notification> inserted = new ArrayList<>();
		for (Long userId : distinctRecipientIds) {
			Notification notification = converter.toNotification(publish, userId);
			notificationMapper.insert(notification);
			inserted.add(notification);
		}
		registerPushAfterCommit(inserted, tenants.captureTenant());
	}

	private void registerPushAfterCommit(List<Notification> notifications, TenantWork tenant) {
		if (notifications.isEmpty()) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			tenant.run(() -> notifications.forEach(n -> pushGateway.push(tenant.tenantId(), n)));
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try { tenant.run(() -> notifications.forEach(n -> pushGateway.push(tenant.tenantId(), n))); }
				catch (RuntimeException error) { log.warn("通知已保存，企业不可用或推送失败 tenantId={}", tenant.tenantId()); }
			}
		});
	}

}

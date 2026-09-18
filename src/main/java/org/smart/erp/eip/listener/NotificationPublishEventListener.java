package org.smart.erp.eip.listener;

import lombok.extern.slf4j.Slf4j;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.event.NotificationPublishEvent;
import org.smart.erp.eip.service.NotificationPersistenceService;
import org.smart.erp.eip.service.NotificationRecipientResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Set;

/** 业务提交后解析收件人并写入通知；通知异常不回滚原业务事务。 */
@Slf4j
@Component
public class NotificationPublishEventListener {

	private final NotificationRecipientResolver recipientResolver;

	private final NotificationPersistenceService persistenceService;

	public NotificationPublishEventListener(NotificationRecipientResolver recipientResolver,
			NotificationPersistenceService persistenceService) {
		this.recipientResolver = recipientResolver;
		this.persistenceService = persistenceService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(NotificationPublishEvent event) {
		try {
			RecipientSelectorDTO selector = event.getPublish().getRecipients();
			Set<Long> recipientIds = recipientResolver
				.resolve(selector == null ? new RecipientSelectorDTO() : selector);
			persistenceService.persist(event, recipientIds);
		}
		catch (RuntimeException exception) {
			log.error("业务事务已提交，但通知处理失败，requestId={}", event.getRequestId(), exception);
		}
	}

}
package org.smart.erp.eip.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.dto.SystemNotificationPublishDTO;
import org.smart.erp.eip.mapper.NotificationTemplateMapper;
import org.smart.erp.eip.service.NotificationPublisher;
import org.smart.erp.eip.service.NotificationRecipientResolver;
import org.smart.erp.eip.service.NotificationTemplateService;
import org.smart.erp.common.security.CurrentUser;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemNotificationServiceImplTests {

	@Mock
	private NotificationTemplateMapper templateMapper;

	@Mock
	private NotificationTemplateService templateService;

	@Mock
	private NotificationRecipientResolver recipientResolver;

	@Mock
	private NotificationPublisher publisher;

	@Mock
	private CurrentUser currentUser;

	@Test
	void rendersOnlyNamedVariables() {
		assertThat(SystemNotificationServiceImpl.render("你好，{{name}}", Map.of("name", "顾威"))).isEqualTo("你好，顾威");
	}

	@Test
	void missingVariableFailsInsteadOfPublishingPartialMessage() {
		assertThatThrownBy(() -> SystemNotificationServiceImpl.render("订单 {{orderNo}} 已完成", Map.of()))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("orderNo");
	}

	@Test
	void rejectsUnsupportedPlaceholderSyntax() {
		assertThatThrownBy(() -> SystemNotificationServiceImpl.render("订单 {{order-no}}", Map.of()))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("仅支持");
	}

	@Test
	void directPublishDefaultsToSelectedRecipientsAndKeepsRequestId() {
		SystemNotificationServiceImpl service = new SystemNotificationServiceImpl(templateMapper, templateService,
				recipientResolver, publisher, currentUser);
		RecipientSelectorDTO recipients = RecipientSelectorDTO.users(Set.of(7L));
		SystemNotificationPublishDTO dto = new SystemNotificationPublishDTO();
		dto.setRequestId("manual-001");
		dto.setTitle("停机通知");
		dto.setContent("产线暂停，请及时处理");
		dto.setRecipients(recipients);
		when(recipientResolver.resolve(recipients)).thenReturn(Set.of(7L));

		assertThat(service.publish(dto)).isEqualTo("manual-001");

		ArgumentCaptor<NotificationPublishDTO> captor = ArgumentCaptor.forClass(NotificationPublishDTO.class);
		verify(publisher).publish(captor.capture());
		assertThat(captor.getValue().getRequestId()).isEqualTo("manual-001");
		assertThat(captor.getValue().getRecipients().getUserIds()).containsExactly(7L);
	}

}

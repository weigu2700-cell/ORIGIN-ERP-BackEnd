package org.smart.erp.eip.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.eip.dto.NotificationBusinessRefDTO;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.event.NotificationPublishEvent;
import org.smart.erp.eip.service.impl.NotificationPublisherImpl;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationInfrastructureTests {
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void publisherEmitsFrozenDtoAsOneDomainEvent() {
        NotificationPublisherImpl publisher = new NotificationPublisherImpl(eventPublisher);
        NotificationPublishDTO dto = NotificationPublishDTO.builder()
                .requestId("req-1")
                .sourceType(NotificationSourceType.BUSINESS)
                .type(NotificationType.TASK)
                .title("待处理")
                .content("请处理")
                .business(NotificationBusinessRefDTO.builder()
                        .businessType("purchase_demand")
                        .businessId(12L)
                        .businessNo("PR-12")
                        .build())
                .recipients(RecipientSelectorDTO.builder()
                        .userIds(Set.of(3L))
                        .permissionCodes(Set.of("purchase:approve"))
                        .build())
                .build();

        publisher.publish(dto);

        ArgumentCaptor<NotificationPublishEvent> captor =
                ArgumentCaptor.forClass(NotificationPublishEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        NotificationPublishEvent event = captor.getValue();
        assertThat(event.getRequestId()).isEqualTo("req-1");
        assertThat(event.getPublish().getBusiness().getBusinessType()).isEqualTo("purchase_demand");
        assertThat(event.getPublish().getRecipients().getUserIds()).containsExactly(3L);
    }
}

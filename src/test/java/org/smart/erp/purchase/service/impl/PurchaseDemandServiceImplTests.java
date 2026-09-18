package org.smart.erp.purchase.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.purchase.dto.PurchaseDemandAddDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.service.NotificationPublisher;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseDemandServiceImplTests {

    @Mock
    private PurchaseDemandMapper purchaseDemandMapper;
    @Mock
    private BusinessNoGenerator businessNoGenerator;
    @Mock
    private NotificationPublisher notificationPublisher;

    private PurchaseDemandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseDemandServiceImpl(
                purchaseDemandMapper, businessNoGenerator, notificationPublisher);
    }

    @Test
    void addPublishesPendingApprovalNotificationAfterInsert() {
        when(businessNoGenerator.generateNo(any(), eq("PR"))).thenReturn("PR-001");
        doAnswer(invocation -> {
            PurchaseDemand demand = invocation.getArgument(0);
            demand.setId(1L);
            return 1;
        }).when(purchaseDemandMapper).insert(any(PurchaseDemand.class));

        PurchaseDemandAddDto dto = new PurchaseDemandAddDto();
        dto.setMaterialId(11L);
        dto.setPurchaseQuantity(new BigDecimal("3"));
        dto.setSourceType(PurchaseDemandSourceType.PRODUCTION_ORDER);
        dto.setSourceNo("MO-001");

        PurchaseDemand result = service.addPurchaseDemand(dto);

        assertThat(result.getStatus()).isEqualTo(PurchaseDemandStatus.DRAFT);
        ArgumentCaptor<NotificationPublishDTO> captor = ArgumentCaptor.forClass(NotificationPublishDTO.class);
        verify(notificationPublisher).publish(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK);
        assertThat(captor.getValue().getTitle()).isEqualTo("待审批采购需求");
        assertThat(captor.getValue().getContent()).isEqualTo("采购需求 PR-001 已创建，请及时审批。");
        assertThat(captor.getValue().getBusiness().getBusinessType()).isEqualTo("PURCHASE_DEMAND_PENDING_APPROVAL");
        assertThat(captor.getValue().getBusiness().getBusinessId()).isEqualTo(1L);
        assertThat(captor.getValue().getBusiness().getBusinessNo()).isEqualTo("PR-001");
        assertThat(captor.getValue().getRecipients().getPermissionCodes())
                .containsExactly("purchase:demand:approve");
        assertThat(captor.getValue().getRecipients().isIncludeAdministrators()).isTrue();
    }

    @Test
    void approvePublishesOrderCreationAndUpdateNotification() {
        PurchaseDemand demand = new PurchaseDemand();
        demand.setId(1L);
        demand.setPurchaseDemandNo("PR-001");
        demand.setStatus(PurchaseDemandStatus.DRAFT);
        when(purchaseDemandMapper.selectById(1L)).thenReturn(demand);
        when(purchaseDemandMapper.updateById(any(PurchaseDemand.class))).thenReturn(1);

        service.approvePurchaseDemand(1L);

        assertThat(demand.getStatus()).isEqualTo(PurchaseDemandStatus.APPROVED);
        ArgumentCaptor<NotificationPublishDTO> captor = ArgumentCaptor.forClass(NotificationPublishDTO.class);
        verify(notificationPublisher).publish(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.BUSINESS);
        assertThat(captor.getValue().getBusiness().getBusinessType()).isEqualTo("PURCHASE_DEMAND_APPROVED");
        assertThat(captor.getValue().getRecipients().getPermissionCodes())
                .containsExactlyInAnyOrder("purchase:order:create", "purchase:order:update");
        assertThat(captor.getValue().getRecipients().isIncludeAdministrators()).isTrue();
    }
}

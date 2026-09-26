package org.smart.erp.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.ai.enums.AiActionStatus;
import org.smart.erp.ai.enums.AiActionType;
import org.smart.erp.ai.model.AiPendingAction;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.service.PurchaseOrderService;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiActionServiceImplTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";
    private static final String ACTION_KEY = "erp:ai:action:" + TOKEN;
    private static final String LOCK_KEY = "erp:ai:action:executing:" + TOKEN;

    @Mock
    private PurchaseOrderService purchaseOrderService;
    @Mock
    private OperationString operationString;

    private AiActionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiActionServiceImpl(purchaseOrderService, operationString);
    }

    @Test
    void executesServerSideActionAndConsumesTokenAfterConfirmation() {
        AiPendingAction pendingAction = pendingAction(7L, 3);
        PurchaseOrder current = new PurchaseOrder();
        current.setId(11L);
        current.setPurchaseOrderNo("PO-001");
        current.setVersion(3);

        when(operationString.setIfAbsent(eq(LOCK_KEY), eq(7L), any(Duration.class))).thenReturn(true);
        when(operationString.get(ACTION_KEY)).thenReturn(pendingAction);
        when(purchaseOrderService.getById(11L)).thenReturn(current);

        service.executePendingAction(TOKEN, 7L);

        verify(purchaseOrderService).approvePurchaseOrder(11L);
        verify(operationString).delete(ACTION_KEY);
        verify(operationString).delete(LOCK_KEY);
    }

    @Test
    void rejectsActionCreatedForAnotherUser() {
        when(operationString.setIfAbsent(eq(LOCK_KEY), eq(8L), any(Duration.class))).thenReturn(true);
        when(operationString.get(ACTION_KEY)).thenReturn(pendingAction(7L, 3));

        assertThatThrownBy(() -> service.executePendingAction(TOKEN, 8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权执行该待确认操作");

        verify(purchaseOrderService, never()).approvePurchaseOrder(any());
        verify(operationString).delete(LOCK_KEY);
    }

    @Test
    void invalidatesActionWhenOrderChangedAfterProposal() {
        PurchaseOrder current = new PurchaseOrder();
        current.setId(11L);
        current.setPurchaseOrderNo("PO-001");
        current.setVersion(4);

        when(operationString.setIfAbsent(eq(LOCK_KEY), eq(7L), any(Duration.class))).thenReturn(true);
        when(operationString.get(ACTION_KEY)).thenReturn(pendingAction(7L, 3));
        when(purchaseOrderService.getById(11L)).thenReturn(current);

        assertThatThrownBy(() -> service.executePendingAction(TOKEN, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购订单已被修改，请重新发起审批");

        verify(operationString).delete(ACTION_KEY);
        verify(purchaseOrderService, never()).approvePurchaseOrder(any());
    }

    private static AiPendingAction pendingAction(Long userId, Integer version) {
        return new AiPendingAction(
                TOKEN,
                userId,
                5L,
                AiActionType.APPROVE_PURCHASE_ORDER,
                11L,
                "PO-001",
                version,
                "审批采购订单 PO-001",
                "确认后将审批采购订单 PO-001，并自动生成采购入库单。",
                AiActionStatus.PENDING,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(9)
        );
    }
}

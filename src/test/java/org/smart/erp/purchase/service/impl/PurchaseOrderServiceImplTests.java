package org.smart.erp.purchase.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.eip.service.NotificationPublisher;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.purchase.dto.PurchaseInStockAddDto;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.enums.PurchaseInStockType;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;
import org.smart.erp.purchase.mapper.PurchaseOrderMapper;
import org.smart.erp.purchase.service.PurchaseInStockService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTests {

    @Mock
    private BusinessNoGenerator businessNoGenerator;
    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private PurchaseDemandMapper purchaseDemandMapper;
    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private PurchaseInStockService purchaseInStockService;
    @Mock
    private NotificationPublisher notificationPublisher;

    private PurchaseOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderServiceImpl(businessNoGenerator, purchaseOrderMapper, purchaseDemandMapper,
                materialMapper, supplierMapper, purchaseInStockService, notificationPublisher);
    }

    @Test
    void approvingOrderCreatesNormalPurchaseInStockDto() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(7L);
        order.setPurchaseOrderNo("PO-007");
        order.setMaterialId(11L);
        order.setSupplierId(22L);
        order.setPlannedQuantity(new BigDecimal("3"));
        order.setUnitPrice(new BigDecimal("12.50"));
        order.setExpectedDeliveryDate(LocalDateTime.of(2026, 9, 30, 0, 0));
        order.setStatus(PurchaseOrderStatus.DRAFT);
        when(purchaseOrderMapper.selectById(7L)).thenReturn(order);
        when(purchaseOrderMapper.updateById(any(PurchaseOrder.class))).thenReturn(1);

        service.approvePurchaseOrder(7L);

        ArgumentCaptor<PurchaseInStockAddDto> captor = ArgumentCaptor.forClass(PurchaseInStockAddDto.class);
        verify(purchaseInStockService).addPurchaseInStock(captor.capture());
        PurchaseInStockAddDto dto = captor.getValue();
        assertThat(dto.getPurchaseOrderId()).isEqualTo(7L);
        assertThat(dto.getPurchaseOrderNo()).isEqualTo("PO-007");
        assertThat(dto.getMaterialId()).isEqualTo(11L);
        assertThat(dto.getSupplierId()).isEqualTo(22L);
        assertThat(dto.getInType()).isEqualTo(PurchaseInStockType.PURCHASE_NORMAL);
        assertThat(dto.getInQuantity()).isEqualByComparingTo("3");
        assertThat(dto.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("37.50");
        assertThat(dto.getDeliveryDate()).isEqualTo(order.getExpectedDeliveryDate());
    }
}

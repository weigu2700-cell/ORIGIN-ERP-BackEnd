package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.production.dto.ProductionDemandAddDto;
import org.smart.erp.production.dto.ProductionOrderAddDto;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;
import org.smart.erp.production.mapper.ProductionDemandMapper;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.service.NotificationPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionDemandServiceImplTests {

    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private MaterialMapper materialMapper;
    @Mock private BusinessNoGenerator businessNoGenerator;
    @Mock private ProductionOrderService productionOrderService;
    @Mock private ProductionDemandMapper productionDemandMapper;
    @Mock private NotificationPublisher notificationPublisher;

    private ProductionDemandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductionDemandServiceImpl(
                salesOrderMapper,
                materialMapper,
                businessNoGenerator,
                productionOrderService,
                productionDemandMapper,
                notificationPublisher);
    }

    @Test
    void salesShortageCarriesTargetWarehouseIntoDemandAndProductionOrder() {
        ProductionDemandAddDto dto = demandDto();
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setOrderNo("SO-001");
        when(productionDemandMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(salesOrderMapper.selectOne(any(Wrapper.class))).thenReturn(salesOrder);
        when(businessNoGenerator.generateNo(any(), any())).thenReturn("PD-001");
        doAnswer(invocation -> {
            ProductionDemand demand = invocation.getArgument(0);
            demand.setId(31L);
            return 1;
        }).when(productionDemandMapper).insert(any(ProductionDemand.class));
        when(productionDemandMapper.updateById(any(ProductionDemand.class))).thenReturn(1);

        service.addProductionDemand(dto);

        ArgumentCaptor<ProductionOrderAddDto> orderCaptor =
                ArgumentCaptor.forClass(ProductionOrderAddDto.class);
        verify(productionOrderService).addProductionOrder(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getProductionDemandId()).isEqualTo(31L);
        assertThat(orderCaptor.getValue().getWarehouseId()).isEqualTo(21L);
        assertThat(orderCaptor.getValue().getPlannedQuantity()).isEqualByComparingTo("5");

        ArgumentCaptor<ProductionDemand> demandCaptor =
                ArgumentCaptor.forClass(ProductionDemand.class);
        verify(productionDemandMapper).updateById(demandCaptor.capture());
        assertThat(demandCaptor.getValue().getStatus()).isEqualTo(ProductionStatus.PLANNED);
        ArgumentCaptor<NotificationPublishDTO> notificationCaptor =
                ArgumentCaptor.forClass(NotificationPublishDTO.class);
        verify(notificationPublisher).publish(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.TASK);
        assertThat(notificationCaptor.getValue().getContent()).contains("PD-001");
        assertThat(notificationCaptor.getValue().getBusiness().getBusinessType())
                .isEqualTo("PRODUCTION_DEMAND_PLANNED");
        assertThat(notificationCaptor.getValue().getBusiness().getBusinessId()).isEqualTo(31L);
        assertThat(notificationCaptor.getValue().getRecipients().getPermissionCodes())
                .containsExactly("production:order:release");
        assertThat(notificationCaptor.getValue().getRecipients().isIncludeAdministrators()).isTrue();
    }

    @Test
    void cancellingSalesOrderCancelsDraftProductionAndDemand() {
        ProductionDemand demand = productionDemand(ProductionStatus.PLANNED);
        ProductionOrder order = productionOrder(ProductionOrderStatus.DRAFT);
        when(productionDemandMapper.selectList(any(Wrapper.class))).thenReturn(List.of(demand));
        when(productionOrderService.list(any(Wrapper.class))).thenReturn(List.of(order));

        service.cancelBySalesOrder("SO-001");

        verify(productionOrderService).cancelProductionOrder(41L);
        verify(productionDemandMapper).updateById(demand);
        assertThat(demand.getStatus()).isEqualTo(ProductionStatus.CANCELLED);
    }

    @Test
    void cancellingSalesOrderIsRejectedAfterProductionWasReleased() {
        ProductionDemand demand = productionDemand(ProductionStatus.PLANNED);
        ProductionOrder order = productionOrder(ProductionOrderStatus.RELEASED);
        when(productionDemandMapper.selectList(any(Wrapper.class))).thenReturn(List.of(demand));
        when(productionOrderService.list(any(Wrapper.class))).thenReturn(List.of(order));

        assertThatThrownBy(() -> service.cancelBySalesOrder("SO-001"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).contains("不可直接取消"));

        verify(productionOrderService, never()).cancelProductionOrder(any());
        verify(productionDemandMapper, never()).updateById(any(ProductionDemand.class));
    }

    private ProductionDemandAddDto demandDto() {
        ProductionDemandAddDto dto = new ProductionDemandAddDto();
        dto.setMaterialId(11L);
        dto.setWarehouseId(21L);
        dto.setQuantity(new BigDecimal("5"));
        dto.setSourceType(ProductionSourceType.SALES_ORDER);
        dto.setSourceNo("SO-001");
        return dto;
    }

    private ProductionDemand productionDemand(ProductionStatus status) {
        ProductionDemand demand = new ProductionDemand();
        demand.setId(31L);
        demand.setSourceType(ProductionSourceType.SALES_ORDER);
        demand.setSourceNo("SO-001");
        demand.setStatus(status);
        return demand;
    }

    private ProductionOrder productionOrder(ProductionOrderStatus status) {
        ProductionOrder order = new ProductionOrder();
        order.setId(41L);
        order.setProductionDemandId(31L);
        order.setProductionOrderNo("PO-001");
        order.setStatus(status);
        return order;
    }
}

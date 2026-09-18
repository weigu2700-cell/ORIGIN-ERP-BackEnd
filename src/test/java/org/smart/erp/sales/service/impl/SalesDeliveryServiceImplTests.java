package org.smart.erp.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.exception.BusinessException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.production.dto.ProductionDemandAddDto;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.entity.SalesDeliveryItem;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.mapper.SalesDeliveryMapper;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.sales.service.SalesDeliveryItemService;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesDeliveryVo;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.service.NotificationPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SalesDeliveryServiceImplTests {

    @Mock
    private SalesDeliveryMapper salesDeliveryMapper;
    @Mock
    private SalesDeliveryItemService salesDeliveryItemService;
    @Mock
    private MaterialStockService materialStockService;
    @Mock
    private ProductionDemandService productionDemandService;
    @Mock
    private BusinessNoGenerator businessNoGenerator;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private SalesOrderMapper salesOrderMapper;
    @Mock
    private SalesOrderItemMapper salesOrderItemMapper;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock deliveryLock;
    @Mock
    private NotificationPublisher notificationPublisher;

    private SalesDeliveryServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(deliveryLock);
        lenient().doReturn(true).when(deliveryLock).tryLock(anyLong(), any());
        lenient().when(deliveryLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(salesDeliveryMapper.updateById(any(SalesDelivery.class))).thenReturn(1);
        service = new SalesDeliveryServiceImpl(
                salesDeliveryMapper,
                salesDeliveryItemService,
                materialStockService,
                productionDemandService,
                businessNoGenerator,
                customerMapper,
                salesOrderMapper,
                salesOrderItemMapper,
                salesOrderService,
                redissonClient,
                notificationPublisher);
    }

    @Test
    void confirmReservesAvailableQuantityAndCreatesProductionDemandForShortfall() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.DRAFT);
        SalesDeliveryItem item = item(new BigDecimal("5"), BigDecimal.ZERO);
        MaterialStock stock = stock(new BigDecimal("2"), BigDecimal.ZERO);

        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);
        when(salesDeliveryItemService.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(materialStockService.getOne(any(), eq(false))).thenReturn(stock);
        when(salesDeliveryItemService.getItemVoByDeliveryIds(anyCollection())).thenReturn(List.of());
        when(customerMapper.selectById(71L)).thenReturn(customer());

        SalesDeliveryVo result = service.confirmSalesDeliveryById(1L);

        assertThat(result.getStatus()).isEqualTo(SalesDeliveryStatus.CONFIRMED);
        assertThat(item.getReservedQuantity()).isEqualByComparingTo("2");
        verify(materialStockService).reserveStock(
                11L, 21L, new BigDecimal("2"),
                "SALES_DELIVERY_RESERVE", "SD-001", "销售预占 关联销售订单 SO-001");

        ArgumentCaptor<ProductionDemandAddDto> demandCaptor =
                ArgumentCaptor.forClass(ProductionDemandAddDto.class);
        verify(productionDemandService).addProductionDemand(demandCaptor.capture());
        assertThat(demandCaptor.getValue().getMaterialId()).isEqualTo(11L);
        assertThat(demandCaptor.getValue().getQuantity()).isEqualByComparingTo("3");
        assertThat(demandCaptor.getValue().getSourceType()).isEqualTo(ProductionSourceType.SALES_ORDER);
        assertThat(demandCaptor.getValue().getSourceNo()).isEqualTo("SO-001");
        verify(salesDeliveryMapper).updateById(delivery);
        ArgumentCaptor<NotificationPublishDTO> notificationCaptor =
                ArgumentCaptor.forClass(NotificationPublishDTO.class);
        verify(notificationPublisher).publish(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.TASK);
        assertThat(notificationCaptor.getValue().getBusiness().getBusinessType())
                .isEqualTo("SALES_DELIVERY_PENDING_OUTBOUND");
        assertThat(notificationCaptor.getValue().getBusiness().getBusinessId()).isEqualTo(1L);
        assertThat(notificationCaptor.getValue().getRecipients().getPermissionCodes())
                .containsExactly("sales:delivery:complete");
        assertThat(notificationCaptor.getValue().getRecipients().isIncludeAdministrators()).isTrue();
    }

    @Test
    void orderConfirmationPreparationCreatesOneDraftDeliveryPerWarehouse() {
        SalesOrder order = new SalesOrder();
        order.setId(51L);
        order.setOrderNo("SO-001");
        order.setCustomerId(71L);
        order.setStatus(SalesOrderStatus.CONFIRMED);
        SalesOrderItem first = orderItem(91L, 21L);
        SalesOrderItem second = orderItem(92L, 22L);
        AtomicLong deliveryIds = new AtomicLong(101L);

        when(salesOrderMapper.selectById(51L)).thenReturn(order);
        when(salesOrderItemMapper.selectList(any())).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            SalesDelivery inserted = invocation.getArgument(0);
            inserted.setId(deliveryIds.getAndIncrement());
            return 1;
        }).when(salesDeliveryMapper).insert(any(SalesDelivery.class));

        service.addDeliveriesForOrder(51L);

        verify(salesDeliveryMapper, times(2)).insert(any(SalesDelivery.class));
        ArgumentCaptor<Collection<SalesDeliveryItem>> itemCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(salesDeliveryItemService).saveBatch(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).hasSize(2);
        assertThat(itemCaptor.getValue()).extracting(SalesDeliveryItem::getWarehouseId)
                .containsExactlyInAnyOrder(21L, 22L);
        assertThat(itemCaptor.getValue()).allSatisfy(item ->
                assertThat(item.getReservedQuantity()).isEqualByComparingTo("0"));
    }

    @Test
    void completeOutboundConsumesReservedStockAndFinishesOrderWhenAllDeliveriesAreComplete() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.CONFIRMED);
        SalesDeliveryItem item = item(new BigDecimal("5"), new BigDecimal("5"));

        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);
        when(salesDeliveryItemService.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(salesDeliveryItemService.getItemVoByDeliveryIds(anyCollection())).thenReturn(List.of());
        when(customerMapper.selectById(71L)).thenReturn(customer());
        when(salesDeliveryMapper.selectList(any())).thenReturn(List.of(delivery));

        SalesDeliveryVo result = service.completeSalesDeliveryById(1L);

        assertThat(result.getStatus()).isEqualTo(SalesDeliveryStatus.COMPLETED);
        assertThat(item.getReservedQuantity()).isEqualByComparingTo("0");
        verify(materialStockService).outboundStock(
                11L, 21L, new BigDecimal("5"),
                "SALES_DELIVERY_OUT", "SD-001", "销售出库 关联销售订单 SO-001");
        verify(salesOrderItemMapper).increaseDeliveredQuantity(91L, new BigDecimal("5"));
        verify(salesOrderService).finishSalesOrderById(51L);
    }

    @Test
    void shortagesForSameMaterialAcrossWarehousesProduceSeparateRequestsAtDeliveryBoundary() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.DRAFT);
        SalesDeliveryItem first = item(new BigDecimal("3"), BigDecimal.ZERO);
        SalesDeliveryItem second = item(new BigDecimal("4"), BigDecimal.ZERO);
        second.setId(82L);
        second.setWarehouseId(22L);
        MaterialStock emptyStock = stock(BigDecimal.ZERO, BigDecimal.ZERO);

        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);
        when(salesDeliveryItemService.list(any(Wrapper.class))).thenReturn(List.of(first, second));
        when(materialStockService.getOne(any(), eq(false))).thenReturn(emptyStock);
        when(salesDeliveryItemService.getItemVoByDeliveryIds(anyCollection())).thenReturn(List.of());
        when(customerMapper.selectById(71L)).thenReturn(customer());

        service.confirmSalesDeliveryById(1L);

        ArgumentCaptor<ProductionDemandAddDto> demandCaptor =
                ArgumentCaptor.forClass(ProductionDemandAddDto.class);
        verify(productionDemandService, times(2)).addProductionDemand(demandCaptor.capture());
        assertThat(demandCaptor.getAllValues()).extracting(ProductionDemandAddDto::getMaterialId)
                .containsOnly(11L);
        assertThat(demandCaptor.getAllValues()).extracting(ProductionDemandAddDto::getQuantity)
                .containsExactlyInAnyOrder(new BigDecimal("3"), new BigDecimal("4"));
        assertThat(demandCaptor.getAllValues()).extracting(ProductionDemandAddDto::getWarehouseId)
                .containsExactlyInAnyOrder(21L, 22L);
    }

    @Test
    void shortagesForSameMaterialAndWarehouseAreAggregatedIntoOneDemand() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.DRAFT);
        SalesDeliveryItem first = item(new BigDecimal("3"), BigDecimal.ZERO);
        SalesDeliveryItem second = item(new BigDecimal("4"), BigDecimal.ZERO);
        second.setId(82L);
        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);
        when(salesDeliveryItemService.list(any(Wrapper.class))).thenReturn(List.of(first, second));
        when(materialStockService.getOne(any(), eq(false)))
                .thenReturn(stock(BigDecimal.ZERO, BigDecimal.ZERO));
        when(salesDeliveryItemService.getItemVoByDeliveryIds(anyCollection())).thenReturn(List.of());
        when(customerMapper.selectById(71L)).thenReturn(customer());

        service.confirmSalesDeliveryById(1L);

        ArgumentCaptor<ProductionDemandAddDto> demandCaptor =
                ArgumentCaptor.forClass(ProductionDemandAddDto.class);
        verify(productionDemandService).addProductionDemand(demandCaptor.capture());
        assertThat(demandCaptor.getValue().getQuantity()).isEqualByComparingTo("7");
        assertThat(demandCaptor.getValue().getWarehouseId()).isEqualTo(21L);
    }

    @Test
    void duplicateProcessingLockRejectsBeforeInventorySideEffects() throws Exception {
        doReturn(false).when(deliveryLock).tryLock(anyLong(), any());

        assertThatThrownBy(() -> service.confirmSalesDeliveryById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(409));

        verify(salesDeliveryItemService, never()).list(any(Wrapper.class));
        verify(salesDeliveryMapper, never()).updateById(any(SalesDelivery.class));
    }

    @Test
    void cancelConfirmedDeliveryReleasesOnlyItsReservedQuantity() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.CONFIRMED);
        SalesDeliveryItem item = item(new BigDecimal("5"), new BigDecimal("2"));

        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);
        when(salesDeliveryItemService.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(salesDeliveryItemService.getItemVoByDeliveryIds(anyCollection())).thenReturn(List.of());
        when(customerMapper.selectById(71L)).thenReturn(customer());

        SalesDeliveryVo result = service.cancelSalesDeliveryById(1L);

        assertThat(result.getStatus()).isEqualTo(SalesDeliveryStatus.CANCELLED);
        assertThat(item.getReservedQuantity()).isEqualByComparingTo("0");
        verify(materialStockService).releaseStock(
                11L, 21L, new BigDecimal("2"),
                "SALES_DELIVERY_RELEASE", "SD-001", "释放预占 关联销售订单 SO-001");
        verify(salesDeliveryMapper).updateById(delivery);
    }

    @Test
    void completeRejectsDeliveryThatIsNotConfirmed() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.DRAFT);
        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);

        assertThatThrownBy(() -> service.completeSalesDeliveryById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(400);
                    assertThat(exception.getMessage()).isEqualTo("仅已确认的发货单可完成出库");
                });
    }

    @Test
    void deliveryLockIsReleasedOnlyAfterTransactionCommit() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.DRAFT);
        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);
        when(salesDeliveryItemService.getItemVoByDeliveryIds(anyCollection())).thenReturn(List.of());
        when(customerMapper.selectById(71L)).thenReturn(customer());

        beginTransactionSynchronization();
        try {
            service.cancelSalesDeliveryById(1L);

            verify(deliveryLock, never()).unlock();
            completeTransactionSynchronization(TransactionSynchronization.STATUS_COMMITTED);
            verify(deliveryLock).unlock();
        } finally {
            clearTransactionSynchronization();
        }
    }

    @Test
    void deliveryLockIsReleasedOnlyAfterTransactionRollback() {
        SalesDelivery delivery = delivery(SalesDeliveryStatus.COMPLETED);
        when(salesDeliveryMapper.selectById(1L)).thenReturn(delivery);

        beginTransactionSynchronization();
        try {
            assertThatThrownBy(() -> service.cancelSalesDeliveryById(1L))
                    .isInstanceOf(BusinessException.class);

            verify(deliveryLock, never()).unlock();
            completeTransactionSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(deliveryLock).unlock();
        } finally {
            clearTransactionSynchronization();
        }
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private void completeTransactionSynchronization(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private SalesDelivery delivery(SalesDeliveryStatus status) {
        SalesDelivery delivery = new SalesDelivery();
        delivery.setId(1L);
        delivery.setDeliveryNo("SD-001");
        delivery.setSalesOrderId(51L);
        delivery.setSalesOrderNo("SO-001");
        delivery.setCustomerId(71L);
        delivery.setStatus(status);
        return delivery;
    }

    private SalesDeliveryItem item(BigDecimal quantity, BigDecimal reservedQuantity) {
        SalesDeliveryItem item = new SalesDeliveryItem();
        item.setId(81L);
        item.setDeliveryId(1L);
        item.setSalesOrderItemId(91L);
        item.setMaterialId(11L);
        item.setWarehouseId(21L);
        item.setQuantity(quantity);
        item.setReservedQuantity(reservedQuantity);
        return item;
    }

    private SalesOrderItem orderItem(Long id, Long warehouseId) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(id);
        item.setSalesOrderId(51L);
        item.setMaterialId(11L);
        item.setWarehouseId(warehouseId);
        item.setQuantity(new BigDecimal("5"));
        return item;
    }

    private MaterialStock stock(BigDecimal onHand, BigDecimal reserved) {
        MaterialStock stock = new MaterialStock();
        stock.setMaterialId(11L);
        stock.setWarehouseId(21L);
        stock.setOnHand(onHand);
        stock.setReserved(reserved);
        return stock;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setId(71L);
        customer.setName("客户 A");
        return customer;
    }
}

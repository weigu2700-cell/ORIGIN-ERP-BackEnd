package org.smart.erp.sales.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.eip.service.NotificationPublisher;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.mapper.SalesDeliveryMapper;
import org.smart.erp.sales.mapper.SalesOrderItemMapper;
import org.smart.erp.sales.mapper.SalesOrderMapper;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.sales.service.SalesOrderItemService;
import org.smart.erp.sales.vo.SalesOrderVo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceImplTests {

	@Mock
	private SalesOrderMapper salesOrderMapper;

	@Mock
	private SalesOrderItemMapper salesOrderItemMapper;

	@Mock
	private CustomerMapper customerMapper;

	@Mock
	private SalesOrderItemService salesOrderItemService;

	@Mock
	private BusinessNoGenerator businessNoGenerator;

	@Mock
	private MaterialMapper materialMapper;

	@Mock
	private WarehouseMapper warehouseMapper;

	@Mock
	private SalesDeliveryMapper salesDeliveryMapper;

	@Mock
	private SalesDeliveryService salesDeliveryService;

	@Mock
	private ProductionDemandService productionDemandService;

	@Mock
	private NotificationPublisher notificationPublisher;

	private SalesOrderServiceImpl service;

	@BeforeEach
	void setUp() {
		lenient().when(salesOrderMapper.updateById(any(SalesOrder.class))).thenReturn(1);
		service = new SalesOrderServiceImpl(salesOrderMapper, salesOrderItemMapper, customerMapper,
				salesOrderItemService, businessNoGenerator, materialMapper, warehouseMapper, salesDeliveryMapper,
				salesDeliveryService, productionDemandService, notificationPublisher);
	}

	@Test
	void confirmingOrderCreatesAndConfirmsEveryDraftDelivery() {
		SalesOrder order = order(SalesOrderStatus.DRAFT);
		SalesDelivery draft = delivery(101L, SalesDeliveryStatus.DRAFT);
		SalesDelivery alreadyConfirmed = delivery(102L, SalesDeliveryStatus.CONFIRMED);

		when(salesOrderMapper.selectById(51L)).thenReturn(order);
		when(salesDeliveryMapper.selectList(any())).thenReturn(List.of(draft, alreadyConfirmed));
		when(customerMapper.selectById(71L)).thenReturn(customer());
		when(salesOrderItemService.getItemBySalesOrderId(51L)).thenReturn(List.of());

		SalesOrderVo result = service.confirmSalesOrderById(51L, null);

		assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.CONFIRMED);
		verify(salesDeliveryService).addDeliveriesForOrder(51L);
		verify(salesDeliveryService).confirmSalesDeliveryInternally(101L);
		verify(salesDeliveryService, never()).confirmSalesDeliveryInternally(102L);
		verify(salesOrderMapper).updateById(order);
	}

	@Test
	void cancellingConfirmedOrderCancelsDraftAndConfirmedDeliveries() {
		SalesOrder order = order(SalesOrderStatus.CONFIRMED);
		SalesDelivery draft = delivery(101L, SalesDeliveryStatus.DRAFT);
		SalesDelivery confirmed = delivery(102L, SalesDeliveryStatus.CONFIRMED);

		when(salesOrderMapper.selectById(51L)).thenReturn(order);
		when(salesDeliveryMapper.selectList(any())).thenReturn(List.of(draft, confirmed));
		when(customerMapper.selectById(71L)).thenReturn(customer());
		when(salesOrderItemService.getItemBySalesOrderId(51L)).thenReturn(List.of());

		SalesOrderVo result = service.cancelSalesOrderById(51L, null);

		assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
		verify(salesDeliveryService).cancelSalesDeliveryInternally(101L);
		verify(salesDeliveryService).cancelSalesDeliveryInternally(102L);
		verify(productionDemandService).cancelBySalesOrder("SO-001");
		verify(salesOrderMapper).updateById(order);
	}

	@Test
	void finishingOrderRequiresAllDeliveriesToBeCompleted() {
		SalesOrder order = order(SalesOrderStatus.CONFIRMED);
		SalesDelivery completed = delivery(101L, SalesDeliveryStatus.COMPLETED);
		SalesDelivery pending = delivery(102L, SalesDeliveryStatus.CONFIRMED);
		when(salesOrderMapper.selectById(51L)).thenReturn(order);
		when(salesDeliveryMapper.selectList(any())).thenReturn(List.of(completed, pending));

		assertThatThrownBy(() -> service.finishSalesOrderById(51L)).isInstanceOfSatisfying(BusinessException.class,
				exception -> {
					assertThat(exception.getCode()).isEqualTo(400);
					assertThat(exception.getMessage()).isEqualTo("存在未出库完成的发货单，无法完成订单");
				});
		verify(salesOrderMapper, never()).updateById(any(SalesOrder.class));
	}

	@Test
	void finishingOrderMarksItCompletedAfterAllDeliveriesComplete() {
		SalesOrder order = order(SalesOrderStatus.CONFIRMED);
		when(salesOrderMapper.selectById(51L)).thenReturn(order);
		when(salesDeliveryMapper.selectList(any())).thenReturn(List.of(delivery(101L, SalesDeliveryStatus.COMPLETED)));

		service.finishSalesOrderById(51L);

		assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.COMPLETED);
		verify(salesOrderMapper).updateById(order);
	}

	private SalesOrder order(SalesOrderStatus status) {
		SalesOrder order = new SalesOrder();
		order.setId(51L);
		order.setOrderNo("SO-001");
		order.setCustomerId(71L);
		order.setStatus(status);
		return order;
	}

	private SalesDelivery delivery(Long id, SalesDeliveryStatus status) {
		SalesDelivery delivery = new SalesDelivery();
		delivery.setId(id);
		delivery.setSalesOrderId(51L);
		delivery.setStatus(status);
		return delivery;
	}

	private Customer customer() {
		Customer customer = new Customer();
		customer.setId(71L);
		customer.setName("客户 A");
		return customer;
	}

}

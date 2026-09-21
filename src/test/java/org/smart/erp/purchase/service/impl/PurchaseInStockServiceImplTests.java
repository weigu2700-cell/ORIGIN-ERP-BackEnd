package org.smart.erp.purchase.service.impl;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.SupplierMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.production.service.ProductionPickingService;
import org.smart.erp.purchase.dto.PurchaseInStockUploadDto;
import org.smart.erp.purchase.dto.PurchaseInStockAddDto;
import org.smart.erp.purchase.entity.PurchaseInStock;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;
import org.smart.erp.purchase.mapper.PurchaseOrderMapper;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.enums.NotificationType;
import org.smart.erp.eip.service.NotificationPublisher;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseInStockServiceImplTests {

	@Mock
	private Validator validator;

	@Mock
	private BusinessNoGenerator businessNoGenerator;

	@Mock
	private MaterialMapper materialMapper;

	@Mock
	private SupplierMapper supplierMapper;

	@Mock
	private WarehouseMapper warehouseMapper;

	@Mock
	private PurchaseOrderMapper purchaseOrderMapper;

	@Mock
	private MaterialStockService materialStockService;

	@Mock
	private ProductionPickingService productionPickingService;

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private NotificationPublisher notificationPublisher;

	private PurchaseInStockServiceImpl service;

	@BeforeEach
	void setUp() {
		service = spy(new PurchaseInStockServiceImpl(validator, businessNoGenerator, materialMapper, supplierMapper,
				warehouseMapper, purchaseOrderMapper, materialStockService, productionPickingService, redissonClient,
				notificationPublisher));
	}

	@Test
	void addPublishesPendingApprovalNotificationAfterInsert() {
		when(businessNoGenerator.generateNo(any(), eq("PI"))).thenReturn("PI-001");
		doAnswer(invocation -> {
			((PurchaseInStock) invocation.getArgument(0)).setId(1L);
			return true;
		}).when(service).save(any(PurchaseInStock.class));
		PurchaseInStockAddDto dto = new PurchaseInStockAddDto();
		dto.setPurchaseOrderId(2L);
		dto.setPurchaseOrderNo("PO-002");
		dto.setMaterialId(11L);
		dto.setInType(PurchaseInStockType.PURCHASE_NORMAL);
		dto.setInQuantity(new BigDecimal("3"));
		dto.setUnitPrice(new BigDecimal("10"));
		when(validator.validate(dto)).thenReturn(Set.of());

		service.addPurchaseInStock(dto);

		ArgumentCaptor<PurchaseInStock> stockCaptor = ArgumentCaptor.forClass(PurchaseInStock.class);
		verify(service).save(stockCaptor.capture());
		assertThat(stockCaptor.getValue().getPurchaseOrderNo()).isEqualTo("PO-002");
		assertThat(stockCaptor.getValue().getInType()).isEqualTo(PurchaseInStockType.PURCHASE_NORMAL);
		assertThat(stockCaptor.getValue().getInDate()).isNull();

		ArgumentCaptor<NotificationPublishDTO> captor = ArgumentCaptor.forClass(NotificationPublishDTO.class);
		verify(notificationPublisher).publish(captor.capture());
		assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK);
		assertThat(captor.getValue().getBusiness().getBusinessType()).isEqualTo("PURCHASE_IN_STOCK_PENDING_APPROVAL");
		assertThat(captor.getValue().getRecipients().getPermissionCodes()).containsExactly("purchase:in:stock:approve");
		assertThat(captor.getValue().getRecipients().isIncludeAdministrators()).isTrue();
	}

	@Test
	void approvalPublishesPendingUploadNotificationAfterStatusUpdate() {
		PurchaseInStock stock = approvedStock();
		stock.setStatus(PurchaseInStockStatus.DRAFT);
		doReturn(stock).when(service).getById(1L);
		doReturn(true).when(service).updateById(any(PurchaseInStock.class));

		service.doApprovePurchaseInStock(1L);

		ArgumentCaptor<NotificationPublishDTO> captor = ArgumentCaptor.forClass(NotificationPublishDTO.class);
		verify(notificationPublisher).publish(captor.capture());
		assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK);
		assertThat(captor.getValue().getBusiness().getBusinessType()).isEqualTo("PURCHASE_IN_STOCK_PENDING_UPLOAD");
		assertThat(captor.getValue().getRecipients().getPermissionCodes()).containsExactly("purchase:in:stock:upload");
		assertThat(captor.getValue().getRecipients().isIncludeAdministrators()).isTrue();
	}

	@Test
	void uploadUsesRequestedWarehouseThenUpdatesStatusAndNotifiesPicking() {
		PurchaseInStock stock = approvedStock();
		PurchaseInStockUploadDto dto = uploadDto(22L);
		when(validator.validate(dto)).thenReturn(Set.of());
		doReturn(stock).when(service).getById(1L);
		doReturn(true).when(service).updateById(any(PurchaseInStock.class));

		service.uploadPurchaseInStock(1L, dto);

		verify(materialStockService).inboundStock(11L, 22L, new BigDecimal("3"), "PURCHASE_IN_STOCK", "PI-001",
				"采购入库上架");
		verify(productionPickingService).notifyPickingForInStock(11L, 22L);
		verify(service).updateById(stock);
		assertThat(stock.getWarehouseId()).isEqualTo(22L);
		assertThat(stock.getStatus()).isEqualTo(PurchaseInStockStatus.UPLOADED);
		assertThat(stock.getStorageLocation()).isEqualTo("A-01");
		assertThat(stock.getInDate()).isNotNull();
	}

	@Test
	void uploadStopsBeforeStatusAndNotificationWhenInventoryInboundFails() {
		PurchaseInStock stock = approvedStock();
		PurchaseInStockUploadDto dto = uploadDto(null);
		when(validator.validate(dto)).thenReturn(Set.of());
		doReturn(stock).when(service).getById(1L);
		doThrow(new BusinessException(409, "入库失败")).when(materialStockService)
			.inboundStock(11L, 21L, new BigDecimal("3"), "PURCHASE_IN_STOCK", "PI-001", "采购入库上架");

		assertThatThrownBy(() -> service.uploadPurchaseInStock(1L, dto)).isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getCode()).isEqualTo(409));

		verify(service, never()).updateById(any(PurchaseInStock.class));
		verify(productionPickingService, never()).notifyPickingForInStock(any(), any());
		assertThat(stock.getStatus()).isEqualTo(PurchaseInStockStatus.APPROVED);
	}

	private PurchaseInStock approvedStock() {
		PurchaseInStock stock = new PurchaseInStock();
		stock.setId(1L);
		stock.setInStockNo("PI-001");
		stock.setMaterialId(11L);
		stock.setWarehouseId(21L);
		stock.setInQuantity(new BigDecimal("3"));
		stock.setStatus(PurchaseInStockStatus.APPROVED);
		return stock;
	}

	private PurchaseInStockUploadDto uploadDto(Long warehouseId) {
		PurchaseInStockUploadDto dto = new PurchaseInStockUploadDto();
		dto.setWarehouseId(warehouseId);
		dto.setStorageLocation("A-01");
		return dto;
	}

}

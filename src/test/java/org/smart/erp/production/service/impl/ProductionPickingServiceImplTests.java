package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.production.dto.ProductionPickingPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.entity.ProductionPicking;
import org.smart.erp.production.enums.ProductionPickingStatus;
import org.smart.erp.production.mapper.ProductionOrderMapper;
import org.smart.erp.production.mapper.ProductionPickingMapper;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.vo.ProductionPickingVo;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionPickingServiceImplTests {

    @Mock
    private BusinessNoGenerator businessNoGenerator;
    @Mock
    private ProductionPickingMapper productionPickingMapper;
    @Mock
    private ProductionOrderMapper productionOrderMapper;
    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private PurchaseDemandMapper purchaseDemandMapper;
    @Mock
    private BOMService bomService;
    @Mock
    private MaterialStockService materialStockService;

    private ProductionPickingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductionPickingServiceImpl(
                businessNoGenerator,
                productionPickingMapper,
                productionOrderMapper,
                materialMapper,
                warehouseMapper,
                purchaseDemandMapper,
                bomService,
                materialStockService);
    }

    @Test
    void detailUsesTheSharedVoEnrichment() {
        ProductionPicking picking = picking();
        when(productionPickingMapper.selectById(1L)).thenReturn(picking);
        when(materialMapper.selectByIds(anyCollection())).thenReturn(List.of(material()));
        when(warehouseMapper.selectByIds(anyCollection())).thenReturn(List.of(warehouse()));
        when(productionOrderMapper.selectByIds(anyCollection())).thenReturn(List.of(order()));
        when(purchaseDemandMapper.selectByIds(anyCollection())).thenReturn(List.of(demand()));

        ProductionPickingVo result = service.getProductionPicking(1L);

        assertThat(result.getPickingNo()).isEqualTo("PICK-001");
        assertThat(result.getMaterialCode()).isEqualTo("MAT-001");
        assertThat(result.getMaterialName()).isEqualTo("螺栓");
        assertThat(result.getWarehouseName()).isEqualTo("原料仓");
        assertThat(result.getProductionOrderNo()).isEqualTo("PO-001");
        assertThat(result.getPurchaseDemandNo()).isEqualTo("PD-001");
        assertThat(result.getStatus()).isEqualTo(ProductionPickingStatus.APPROVED);
    }

    @Test
    void missingDetailReturnsNotFoundBusinessCode() {
        when(productionPickingMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getProductionPicking(99L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(404);
                    assertThat(exception.getMessage()).isEqualTo("领料单不存在");
                });
    }

    @Test
    void pageStatusAcceptsStableNumericCodeAndQueriesWithEnumValue() {
        ProductionPickingPageDto dto = new ProductionPickingPageDto();
        dto.setStatus(ProductionPickingStatus.APPROVED.getCode());
        when(productionPickingMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<ProductionPicking> page = invocation.getArgument(0);
            page.setRecords(List.of());
            return page;
        });

        service.pageProductionPicking(dto);

        ArgumentCaptor<Wrapper<ProductionPicking>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(productionPickingMapper).selectPage(any(Page.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getParamNameValuePairs().values())
                .contains(ProductionPickingStatus.APPROVED);
    }

    @Test
    void pageRejectsUnknownStatusCode() {
        ProductionPickingPageDto dto = new ProductionPickingPageDto();
        dto.setStatus(99);

        assertThatThrownBy(() -> service.pageProductionPicking(dto))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(400));
    }

    private ProductionPicking picking() {
        ProductionPicking picking = new ProductionPicking();
        picking.setId(1L);
        picking.setPickingNo("PICK-001");
        picking.setMaterialId(11L);
        picking.setWarehouseId(21L);
        picking.setProductionOrderId(31L);
        picking.setPurchaseDemandId(41L);
        picking.setStatus(ProductionPickingStatus.APPROVED);
        return picking;
    }

    private Material material() {
        Material material = new Material();
        material.setId(11L);
        material.setCode("MAT-001");
        material.setName("螺栓");
        return material;
    }

    private Warehouse warehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(21L);
        warehouse.setName("原料仓");
        return warehouse;
    }

    private ProductionOrder order() {
        ProductionOrder order = new ProductionOrder();
        order.setId(31L);
        order.setProductionOrderNo("PO-001");
        return order;
    }

    private PurchaseDemand demand() {
        PurchaseDemand demand = new PurchaseDemand();
        demand.setId(41L);
        demand.setPurchaseDemandNo("PD-001");
        return demand;
    }
}

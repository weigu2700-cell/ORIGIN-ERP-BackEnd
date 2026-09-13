package org.smart.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smart.erp.inventory.dto.MaterialStockPageDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.mapper.MaterialStockMapper;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.MaterialStockVo;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialStockServiceImplTests {

    @Mock
    private MaterialStockMapper materialStockMapper;
    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private TransactionService transactionService;

    private MaterialStockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaterialStockServiceImpl(
                materialStockMapper, materialMapper, warehouseMapper, transactionService);
    }

    @Test
    void keywordMatchesMaterialCodeOrNameAndPageEnrichmentIsBatched() {
        MaterialStockPageDto dto = new MaterialStockPageDto();
        dto.setKeyword(" steel ");

        Material material = material(11L, "MAT-STEEL", "不锈钢");
        Warehouse warehouse = warehouse(21L, "原料仓");
        MaterialStock stock = stock(31L, material.getId(), warehouse.getId());

        when(materialMapper.selectList(any())).thenReturn(List.of(material));
        when(materialStockMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<MaterialStock> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(stock));
            return page;
        });
        when(materialMapper.selectByIds(anyCollection())).thenReturn(List.of(material));
        when(warehouseMapper.selectByIds(anyCollection())).thenReturn(List.of(warehouse));

        Page<MaterialStockVo> result = service.pageMaterialStock(dto);

        assertThat(result.getRecords()).singleElement().satisfies(vo -> {
            assertThat(vo.getMaterialCode()).isEqualTo("MAT-STEEL");
            assertThat(vo.getMaterialName()).isEqualTo("不锈钢");
            assertThat(vo.getWarehouseName()).isEqualTo("原料仓");
            assertThat(vo.getAvailable()).isEqualByComparingTo("8");
        });

        ArgumentCaptor<Wrapper<Material>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(materialMapper).selectList(queryCaptor.capture());
        String sql = queryCaptor.getValue().getSqlSegment();
        assertThat(sql).contains("code LIKE").contains("name LIKE").contains("OR");
        assertThat(queryCaptor.getValue().getParamNameValuePairs().values()).contains("%steel%");
        verify(materialMapper, never()).selectById(any());
        verify(warehouseMapper, never()).selectById(any());
    }

    @Test
    void materialCodeRemainsAnExactFilter() {
        MaterialStockPageDto dto = new MaterialStockPageDto();
        dto.setMaterialCode(" MAT-001 ");
        when(materialMapper.selectList(any())).thenReturn(List.of());

        Page<MaterialStockVo> result = service.pageMaterialStock(dto);

        assertThat(result.getRecords()).isEmpty();
        ArgumentCaptor<Wrapper<Material>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(materialMapper).selectList(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment()).contains("code =").doesNotContain("LIKE");
        assertThat(queryCaptor.getValue().getParamNameValuePairs().values()).contains("MAT-001");
        verify(materialStockMapper, never()).selectPage(any(Page.class), any());
    }

    private Material material(Long id, String code, String name) {
        Material material = new Material();
        material.setId(id);
        material.setCode(code);
        material.setName(name);
        return material;
    }

    private Warehouse warehouse(Long id, String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName(name);
        return warehouse;
    }

    private MaterialStock stock(Long id, Long materialId, Long warehouseId) {
        MaterialStock stock = new MaterialStock();
        stock.setId(id);
        stock.setMaterialId(materialId);
        stock.setWarehouseId(warehouseId);
        stock.setOnHand(new BigDecimal("10"));
        stock.setReserved(new BigDecimal("2"));
        return stock;
    }
}

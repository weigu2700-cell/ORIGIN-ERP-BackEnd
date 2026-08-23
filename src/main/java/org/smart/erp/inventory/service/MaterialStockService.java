package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.inventory.dto.materialStockDto.CreateDto;
import org.smart.erp.inventory.dto.materialStockDto.ListDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.vo.MaterialStockVO;

import java.math.BigDecimal;

public interface MaterialStockService extends IService<MaterialStock> {
    MaterialStockVO createMaterialStock(CreateDto dto);

    Page<MaterialStockVO> listMaterialStock(ListDto dto);

    MaterialStockVO getMaterialStock(Long id);

    void reserveStock(Long materialId, Long warehouseId, BigDecimal quantity);

    void releaseStock(Long materialId, Long warehouseId, BigDecimal quantity);

    void outboundStock(Long materialId, Long warehouseId, BigDecimal quantity);

    void inboundStock(Long materialId, Long warehouseId, BigDecimal quantity);
}

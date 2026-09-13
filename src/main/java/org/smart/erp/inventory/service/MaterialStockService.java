package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.inventory.dto.MaterialStockAddDto;
import org.smart.erp.inventory.dto.MaterialStockPageDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.vo.MaterialStockVo;

import java.math.BigDecimal;

public interface MaterialStockService extends IService<MaterialStock> {
    MaterialStockVo addMaterialStock(MaterialStockAddDto dto);

    Page<MaterialStockVo> pageMaterialStock(MaterialStockPageDto dto);

    MaterialStockVo getMaterialStock(Long id);

    void reserveStock(Long materialId, Long warehouseId, BigDecimal quantity,
                      String businessType, String businessNo, String remark);

    void releaseStock(Long materialId, Long warehouseId, BigDecimal quantity,
                      String businessType, String businessNo, String remark);

    void outboundStock(Long materialId, Long warehouseId, BigDecimal quantity,
                       String businessType, String businessNo, String remark);

    void inboundStock(Long materialId, Long warehouseId, BigDecimal quantity,
                      String businessType, String businessNo, String remark);
}

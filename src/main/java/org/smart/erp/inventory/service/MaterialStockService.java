package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.inventory.dto.MaterialStockAddDto;
import org.smart.erp.inventory.dto.MaterialStockPageDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.vo.MaterialStockVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;

public interface MaterialStockService extends IService<MaterialStock> {
    @PreAuthorize("hasAnyAuthority('inventory:material-stock:create')")
    MaterialStockVo addMaterialStock(MaterialStockAddDto dto);

    @PreAuthorize("hasAnyAuthority('inventory:material-stock:list')")
    Page<MaterialStockVo> pageMaterialStock(MaterialStockPageDto dto);

    @PreAuthorize("hasAnyAuthority('inventory:material-stock:get')")
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

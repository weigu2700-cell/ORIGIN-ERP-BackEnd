package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.ProductionOrderAddDto;
import org.smart.erp.production.dto.ProductionOrderPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionOrderVo;

import java.util.List;

public interface ProductionOrderService extends IService<ProductionOrder> {
    void addProductionOrder(ProductionOrderAddDto dto);

    Page<ProductionOrderVo> pageProductionOrder(ProductionOrderPageDto dto);

    ProductionOrderVo DetailProductionOrder(Long id);

    void startProductionOrder(Long id);

    void completeProductionOrder(Long id);

    void cancelProductionOrder(Long id);

    List<MaterialRequirementVo> releaseProductionOrder(Long id);
}

package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.createProductionOrderDto;
import org.smart.erp.production.dto.pageProductionOrderDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionOrderVo;

import java.util.List;

public interface ProductionOrderService extends IService<ProductionOrder> {
    void createProductionOrder(createProductionOrderDto dto);

    Page<ProductionOrderVo> pageProductionOrder(pageProductionOrderDto dto);

    ProductionOrderVo DetailProductionOrder(Long id);

    void startProductionOrder(Long id);

    void completeProductionOrder(Long id);

    void cancelProductionOrder(Long id);

    List<MaterialRequirementVo> releaseProductionOrder(Long id);
}

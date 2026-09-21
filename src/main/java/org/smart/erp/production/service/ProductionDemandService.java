package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.production.dto.ProductionDemandAddDto;
import org.smart.erp.production.dto.ProductionDemandPageDto;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.vo.ProductionDemandVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProductionDemandService extends IService<ProductionDemand> {

    void addProductionDemand(ProductionDemandAddDto dto);

    @PreAuthorize("hasAnyAuthority('production:demand:list')")
    Page<ProductionDemandVo> pageProductionDemand(ProductionDemandPageDto dto);

    @PreAuthorize("hasAnyAuthority('production:demand:get')")
    ProductionDemandVo DetailProductionDemand(Long id);

    /** 取消销售订单仍未下达生产的需求；已进入生产流程时拒绝取消销售订单。 */
    void cancelBySalesOrder(String salesOrderNo);
}

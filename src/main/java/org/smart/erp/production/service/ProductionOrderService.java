package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.production.dto.ProductionOrderAddDto;
import org.smart.erp.production.dto.ProductionOrderPageDto;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionOrderVo;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProductionOrderService extends IService<ProductionOrder> {
    @PreAuthorize("hasAnyAuthority('production:order:create')")
    void addProductionOrder(ProductionOrderAddDto dto);

    @PreAuthorize("hasAnyAuthority('production:order:list')")
    Page<ProductionOrderVo> pageProductionOrder(ProductionOrderPageDto dto);

    @PreAuthorize("hasAnyAuthority('production:order:get')")
    ProductionOrderVo DetailProductionOrder(Long id);

    @PreAuthorize("hasAnyAuthority('production:order:start')")
    void startProductionOrder(Long id);

    @PreAuthorize("hasAnyAuthority('production:order:complete')")
    void completeProductionOrder(Long id);

    @PreAuthorize("hasAnyAuthority('production:order:cancel')")
    void cancelProductionOrder(Long id);

    @PreAuthorize("hasAnyAuthority('production:order:release')")
    List<MaterialRequirementVo> releaseProductionOrder(Long id);
}

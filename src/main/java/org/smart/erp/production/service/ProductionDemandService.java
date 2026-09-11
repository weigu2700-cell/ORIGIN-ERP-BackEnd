package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.ProductionDemandAddDto;
import org.smart.erp.production.dto.ProductionDemandPageDto;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.vo.ProductionDemandVo;

public interface ProductionDemandService extends IService<ProductionDemand> {

    void addProductionDemand(ProductionDemandAddDto dto);

    Page<ProductionDemandVo> pageProductionDemand(ProductionDemandPageDto dto);

    ProductionDemandVo DetailProductionDemand(Long id);
}

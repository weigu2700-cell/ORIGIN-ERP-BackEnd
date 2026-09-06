package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.createProductionDemandDto;
import org.smart.erp.production.dto.pageProductionDemandDto;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.vo.ProductionDemandVo;

public interface ProductionDemandService extends IService<ProductionDemand> {

    void createProductionDemand(createProductionDemandDto dto);

    Page<ProductionDemandVo> pageProductionDemand(pageProductionDemandDto dto);

    ProductionDemandVo DetailProductionDemand(Long id);
}

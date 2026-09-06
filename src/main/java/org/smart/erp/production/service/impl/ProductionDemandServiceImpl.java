package org.smart.erp.production.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.mapper.ProductionDemandMapper;
import org.smart.erp.production.service.ProductionDemandService;
import org.springframework.stereotype.Service;

@Service
public class ProductionDemandServiceImpl
    extends ServiceImpl<ProductionDemandMapper, ProductionDemand> implements ProductionDemandService
{
}

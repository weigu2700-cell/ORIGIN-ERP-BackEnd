package org.smart.erp.production.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.production.dto.createProductionDemandDto;
import org.smart.erp.production.entity.ProductionDemand;

@Mapper
public interface ProductionDemandMapper extends BaseMapper<ProductionDemand> {
}

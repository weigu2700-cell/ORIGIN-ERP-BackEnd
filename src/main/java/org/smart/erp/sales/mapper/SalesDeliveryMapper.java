package org.smart.erp.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.sales.dto.salesDeliveryDto.CreateDto;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.vo.SalesDeliveryVo;

@Mapper
public interface SalesDeliveryMapper extends BaseMapper<SalesDelivery> {

}

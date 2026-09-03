package org.smart.erp.sales.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesDeliveryDto.CreateDto;
import org.smart.erp.sales.dto.salesDeliveryDto.ListDto;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.vo.SalesDeliveryVo;

public interface SalesDeliveryService extends IService<SalesDelivery> {

    SalesDeliveryVo createSalesDeliveryVo(CreateDto dto);

    SalesDeliveryVo getSalesDeliveryVoById(Long id);

    SalesDeliveryVo confirmSalesDeliveryById(Long id);

    SalesDeliveryVo cancelSalesDeliveryById(Long id);

    Page<SalesDeliveryVo> getPageSalesDeliveryVo(ListDto dto);
}

package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.vo.SalesOrderItemVo;

public interface SalesOrderItemService extends IService<SalesOrderItem> {

    SalesOrderItemVo createItem(SalesOrderItem dto);
}

package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesOrderItemDto.createItemDto;
import org.smart.erp.sales.dto.salesOrderItemDto.updateItemDto;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.vo.SalesOrderItemVo;

import java.util.List;

public interface SalesOrderItemService extends IService<SalesOrderItem> {

    SalesOrderItemVo createItem(SalesOrderItem dto);

    List<SalesOrderItemVo> getItemBySalesOrderId(Long salesOrderId);

    void updateItemBySalesOrderId(Long id, List<updateItemDto> items);

    void removeItemBySalesOrderId(Long id);
}

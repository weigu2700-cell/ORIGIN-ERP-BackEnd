package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesOrderItemDto.SalesOrderItemAddDto;
import org.smart.erp.sales.dto.salesOrderItemDto.SalesOrderItemUpdateDto;
import org.smart.erp.sales.entity.SalesOrderItem;
import org.smart.erp.sales.vo.SalesOrderItemVo;

import java.util.List;

public interface SalesOrderItemService extends IService<SalesOrderItem> {

    SalesOrderItemVo addItem(SalesOrderItem dto);

    List<SalesOrderItemVo> getItemBySalesOrderId(Long salesOrderId);

    void updateItemBySalesOrderId(Long id, List<SalesOrderItemUpdateDto> items);

    void removeItemBySalesOrderId(Long id);
}

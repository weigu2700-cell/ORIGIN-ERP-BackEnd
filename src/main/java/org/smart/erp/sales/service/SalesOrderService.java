package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
import org.smart.erp.sales.dto.salesOrderDto.listDto;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.vo.SalesOrderVo;

public interface SalesOrderService extends IService<SalesOrder> {
    SalesOrderVo create(createDto dto);

    Page<SalesOrderVo> listSalesOrderVoByPage(listDto dto);

    SalesOrderVo getSalesOrderVoById(Long id);
}

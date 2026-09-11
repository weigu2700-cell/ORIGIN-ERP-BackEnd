package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderAddDto;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderPageDto;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderUpdateDto;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.vo.SalesOrderVo;

public interface SalesOrderService extends IService<SalesOrder> {
    SalesOrderVo add(SalesOrderAddDto dto);

    Page<SalesOrderVo> pageSalesOrderVoByPage(SalesOrderPageDto dto);

    SalesOrderVo detailSalesOrderVo(Long id);

    SalesOrderVo updateSalesOrderVoById(Long id, SalesOrderUpdateDto dto);

    void removeSalesOrderById(Long id);

    SalesOrderVo confirmSalesOrderById(Long id, SalesOrderUpdateDto dto);

    SalesOrderVo cancelSalesOrderById(Long id, SalesOrderUpdateDto dto);

    void finishSalesOrderById(Long id);
}

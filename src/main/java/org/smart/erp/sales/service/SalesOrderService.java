package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderAddDto;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderPageDto;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderUpdateDto;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SalesOrderService extends IService<SalesOrder> {
    @PreAuthorize("hasAnyAuthority('sales:order:create')")
    SalesOrderVo add(SalesOrderAddDto dto);

    @PreAuthorize("hasAnyAuthority('sales:order:list')")
    Page<SalesOrderVo> pageSalesOrderVoByPage(SalesOrderPageDto dto);

    @PreAuthorize("hasAnyAuthority('sales:order:get')")
    SalesOrderVo detailSalesOrderVo(Long id);

    @PreAuthorize("hasAnyAuthority('sales:order:update')")
    SalesOrderVo updateSalesOrderVoById(Long id, SalesOrderUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('sales:order:delete')")
    void removeSalesOrderById(Long id);

    @PreAuthorize("hasAnyAuthority('sales:order:confirm')")
    SalesOrderVo confirmSalesOrderById(Long id, SalesOrderUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('sales:order:cancel')")
    SalesOrderVo cancelSalesOrderById(Long id, SalesOrderUpdateDto dto);

    void finishSalesOrderById(Long id);
}

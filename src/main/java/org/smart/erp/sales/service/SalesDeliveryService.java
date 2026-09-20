package org.smart.erp.sales.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.sales.dto.salesDeliveryDto.SalesDeliveryAddDto;
import org.smart.erp.sales.dto.salesDeliveryDto.SalesDeliveryPageDto;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.vo.SalesDeliveryVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SalesDeliveryService extends IService<SalesDelivery> {

    @PreAuthorize("hasAnyAuthority('sales:delivery:create')")
    SalesDeliveryVo addSalesDeliveryVo(SalesDeliveryAddDto dto);

    /** 创建销售订单时按仓库批量生成草稿态出货单 */
    void addDeliveriesForOrder(Long salesOrderId);

    @PreAuthorize("hasAnyAuthority('sales:delivery:get')")
    SalesDeliveryVo detailSalesDeliveryVo(Long id);

    @PreAuthorize("hasAnyAuthority('sales:delivery:confirm')")
    SalesDeliveryVo confirmSalesDeliveryById(Long id);

    /** Internal order orchestration entry; keeps the order permission boundary. */
    SalesDeliveryVo confirmSalesDeliveryInternally(Long id);

    @PreAuthorize("hasAnyAuthority('sales:delivery:complete')")
    SalesDeliveryVo completeSalesDeliveryById(Long id);

    @PreAuthorize("hasAnyAuthority('sales:delivery:cancel')")
    SalesDeliveryVo cancelSalesDeliveryById(Long id);

    /** Internal order orchestration entry; keeps the order permission boundary. */
    SalesDeliveryVo cancelSalesDeliveryInternally(Long id);

    @PreAuthorize("hasAnyAuthority('sales:delivery:list')")
    Page<SalesDeliveryVo> getPageSalesDeliveryVo(SalesDeliveryPageDto dto);
}

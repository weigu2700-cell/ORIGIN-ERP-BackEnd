package org.smart.erp.sales.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesDeliveryDto.SalesDeliveryAddDto;
import org.smart.erp.sales.dto.salesDeliveryDto.SalesDeliveryPageDto;
import org.smart.erp.sales.entity.SalesDelivery;
import org.smart.erp.sales.vo.SalesDeliveryVo;

public interface SalesDeliveryService extends IService<SalesDelivery> {

    SalesDeliveryVo addSalesDeliveryVo(SalesDeliveryAddDto dto);

    /** 创建销售订单时按仓库批量生成草稿态出货单 */
    void addDeliveriesForOrder(Long salesOrderId);

    SalesDeliveryVo detailSalesDeliveryVo(Long id);

    SalesDeliveryVo confirmSalesDeliveryById(Long id);

    SalesDeliveryVo completeSalesDeliveryById(Long id);

    SalesDeliveryVo cancelSalesDeliveryById(Long id);

    Page<SalesDeliveryVo> getPageSalesDeliveryVo(SalesDeliveryPageDto dto);
}

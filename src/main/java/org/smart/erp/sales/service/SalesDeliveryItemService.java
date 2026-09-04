package org.smart.erp.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.sales.dto.salesDeliveryItemDto.CreateItemDto;
import org.smart.erp.sales.entity.SalesDeliveryItem;
import org.smart.erp.sales.vo.SalesDeliveryItemVo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SalesDeliveryItemService extends IService<SalesDeliveryItem> {

    SalesDeliveryItemVo createSalesDeliveryItemVo(CreateItemDto dto, Long deliveryId, Integer lineNo);

    /**
     * 按发货单 id 批量查询明细 VO（含物料名称/编码、仓库名称），供列表页一次性装配。
     */
    List<SalesDeliveryItemVo> getItemVoByDeliveryIds(Collection<Long> deliveryIds);

    /**
     * 汇总各销售订单明细已发货数量（仅统计未取消的发货单），用于校验剩余可出货量。
     */
    Map<Long, BigDecimal> sumDeliveredQuantityByOrderItemIds(Collection<Long> orderItemIds);
}

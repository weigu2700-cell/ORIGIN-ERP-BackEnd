package org.smart.erp.sales.service;

import org.smart.erp.sales.vo.SalesDeliveryVo;

/** Internal state transition hooks used only by sales order orchestration. */
public interface SalesDeliveryInternalService {
    SalesDeliveryVo confirmSalesDeliveryInternally(Long id);

    SalesDeliveryVo cancelSalesDeliveryInternally(Long id);
}

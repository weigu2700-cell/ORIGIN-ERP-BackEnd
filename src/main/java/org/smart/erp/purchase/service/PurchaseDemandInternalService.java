package org.smart.erp.purchase.service;

import org.smart.erp.purchase.dto.PurchaseDemandAddDto;
import org.smart.erp.purchase.entity.PurchaseDemand;

/** Internal creation hook used by production order orchestration. */
public interface PurchaseDemandInternalService {
    PurchaseDemand addPurchaseDemandInternally(PurchaseDemandAddDto dto);
}

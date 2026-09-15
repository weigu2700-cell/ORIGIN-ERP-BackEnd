package org.smart.erp.system.entity;

import lombok.Data;
import org.smart.erp.production.vo.ProductionOrderVo;
import org.smart.erp.purchase.vo.PurchaseOrderVo;

import java.util.List;
import java.util.Map;

@Data
public class Dashboard {

    private Long pendingProductionDemandCount;

    private Map<String, Long> productionOrderStatusCount;

    private Long draftPurchaseOrderCount;

    private Long shippedPurchaseOrderCount;

    private Long confirmedSalesOrderCount;

    private List<ProductionOrderVo> recentProductionOrders;

    private List<PurchaseOrderVo> recentPurchaseOrders;
}

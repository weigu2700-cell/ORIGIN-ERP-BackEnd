package org.smart.erp.system.entity;

import lombok.Data;
import org.smart.erp.production.vo.ProductionOrderVo;
import org.smart.erp.purchase.vo.PurchaseOrderVo;

import java.util.List;
import java.util.Map;

@Data
public class Dashboard {

    // 进行中的生产订单数（原字段名 pendingProductionDemandCount 与实际口径不符，已更名）
    private Long inProgressProductionOrderCount;

    private Map<String, Long> productionOrderStatusCount;

    private Long draftPurchaseOrderCount;

    private Long shippedPurchaseOrderCount;

    private Long confirmedSalesOrderCount;

    private List<ProductionOrderVo> recentProductionOrders;

    private List<PurchaseOrderVo> recentPurchaseOrders;
}

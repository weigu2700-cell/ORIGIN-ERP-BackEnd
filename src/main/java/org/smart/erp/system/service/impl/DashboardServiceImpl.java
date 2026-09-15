package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.production.entity.ProductionDemand;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.enums.ProductionOrderStatus;
import org.smart.erp.production.enums.ProductionStatus;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.production.vo.ProductionOrderVo;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.purchase.vo.PurchaseOrderVo;
import org.smart.erp.sales.entity.SalesOrder;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.system.cache.DashboardRedis;
import org.smart.erp.system.vo.DashboardVo;
import org.smart.erp.system.service.DashboardService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService
{
    private final ProductionOrderService productionOrderService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final ProductionDemandService productionDemandService;
    private final DashboardRedis dashboardRedis;

    public DashboardServiceImpl(
            ProductionOrderService productionOrderService,
            PurchaseOrderService purchaseOrderService,
            SalesOrderService salesOrderService,
            ProductionDemandService productionDemandService,
            DashboardRedis dashboardRedis
    ) {
        this.productionOrderService = productionOrderService;
        this.purchaseOrderService = purchaseOrderService;
        this.salesOrderService = salesOrderService;
        this.productionDemandService = productionDemandService;
        this.dashboardRedis = dashboardRedis;
    }

    @Override
    public DashboardVo getDashboard() {
        DashboardVo dashboardVo = dashboardRedis.getDashboardCache();
        if (dashboardVo != null) return dashboardVo;

        DashboardVo newDashboardVo = buildDashboard();
        dashboardRedis.activeDashboardCache(newDashboardVo);
        return newDashboardVo;
    }


    /** 统计各生产订单状态的数量（key 为状态 code） */
    private Map<String, Long> buildProductionOrderStatusCount() {
        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (ProductionOrderStatus status : ProductionOrderStatus.values()) {
            long count = productionOrderService.count(
                    new LambdaQueryWrapper<ProductionOrder>().eq(ProductionOrder::getStatus, status));
            statusCount.put(String.valueOf(status.getCode()), count);
        }
        return statusCount;
    }

    private DashboardVo buildDashboard() {
        DashboardVo dashboardVo = new DashboardVo();

        dashboardVo.setPendingProductionDemandCount(productionDemandService.count(
                new LambdaQueryWrapper<ProductionDemand>()
                        .eq(ProductionDemand::getStatus, ProductionStatus.PENDING))
        );

        dashboardVo.setConfirmedSalesOrderCount( salesOrderService.count(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getStatus, SalesOrderStatus.CONFIRMED))
        );

        dashboardVo.setDraftPurchaseOrderCount(purchaseOrderService.count(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, PurchaseOrderStatus.DRAFT))
        );

        dashboardVo.setShippedPurchaseOrderCount(purchaseOrderService.count(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, PurchaseOrderStatus.SHIPPED))
        );

        dashboardVo.setProductionOrderStatusCount(buildProductionOrderStatusCount());

        // 最近订单：按创建时间倒序取前 4 条
        dashboardVo.setRecentProductionOrders(
                productionOrderService.page(new Page<>(1, 4),
                                new LambdaQueryWrapper<ProductionOrder>().orderByDesc(ProductionOrder::getCreateTime))
                        .getRecords().stream()
                        .map(this::toProductionOrderVo)
                        .toList());

        dashboardVo.setRecentPurchaseOrders(
                purchaseOrderService.page(new Page<>(1, 4),
                                new LambdaQueryWrapper<PurchaseOrder>().orderByDesc(PurchaseOrder::getCreateTime))
                        .getRecords().stream()
                        .map(this::toPurchaseOrderVo)
                        .toList());

        return dashboardVo;
    }

    private ProductionOrderVo toProductionOrderVo(ProductionOrder order) {
        ProductionOrderVo vo = new ProductionOrderVo();
        BeanUtils.copyProperties(order, vo);
        vo.setStatus(order.getStatus() == null ? null : order.getStatus().getCode());
        return vo;
    }

    private PurchaseOrderVo toPurchaseOrderVo(PurchaseOrder order) {
        PurchaseOrderVo vo = new PurchaseOrderVo();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }
}




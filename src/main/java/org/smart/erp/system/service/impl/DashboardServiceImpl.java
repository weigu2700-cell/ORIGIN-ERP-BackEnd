package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.production.entity.ProductionOrder;
import org.smart.erp.production.enums.ProductionOrderStatus;
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
import org.smart.erp.system.entity.Dashboard;
import org.smart.erp.system.mapper.DashboardMapper;
import org.smart.erp.system.service.DashboardService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl
        extends ServiceImpl<DashboardMapper, Dashboard>
        implements DashboardService
{
    private final ProductionOrderService productionOrderService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final DashboardRedis dashboardRedis;
    private final CurrentUser currentUser;

    public DashboardServiceImpl(
            ProductionOrderService productionOrderService,
            PurchaseOrderService purchaseOrderService,
            SalesOrderService salesOrderService,
            DashboardRedis dashboardRedis,
            CurrentUser currentUser
    ) {
        this.productionOrderService = productionOrderService;
        this.purchaseOrderService = purchaseOrderService;
        this.salesOrderService = salesOrderService;
        this.dashboardRedis = dashboardRedis;
        this.currentUser = currentUser;
    }

    @Override
    public Dashboard getDashboard() {
        Long userId = currentUser.getUserId();

        Dashboard dashboard = dashboardRedis.getDashboardCache(userId);
        if (dashboard != null) return dashboard;

        Dashboard newDashboard = buildDashboard();
        dashboardRedis.activeDashboardCache(newDashboard, userId, Duration.ofMinutes(1));
        return newDashboard;
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

    private Dashboard buildDashboard() {
        Dashboard dashboard = new Dashboard();

        dashboard.setConfirmedSalesOrderCount( salesOrderService.count(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getStatus, SalesOrderStatus.CONFIRMED))
        );

        dashboard.setDraftPurchaseOrderCount(purchaseOrderService.count(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, PurchaseOrderStatus.DRAFT))
        );

        dashboard.setShippedPurchaseOrderCount(purchaseOrderService.count(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, PurchaseOrderStatus.SHIPPED))
        );

        dashboard.setInProgressProductionOrderCount( productionOrderService.count(
                new LambdaQueryWrapper<ProductionOrder>()
                        .eq(ProductionOrder::getStatus, ProductionOrderStatus.IN_PROGRESS))
        );

        dashboard.setProductionOrderStatusCount(buildProductionOrderStatusCount());

        // 最近订单：按创建时间倒序取前 4 条
        dashboard.setRecentProductionOrders(
                productionOrderService.page(new Page<>(1, 4),
                                new LambdaQueryWrapper<ProductionOrder>().orderByDesc(ProductionOrder::getCreateTime))
                        .getRecords().stream()
                        .map(this::toProductionOrderVo)
                        .toList());

        dashboard.setRecentPurchaseOrders(
                purchaseOrderService.page(new Page<>(1, 4),
                                new LambdaQueryWrapper<PurchaseOrder>().orderByDesc(PurchaseOrder::getCreateTime))
                        .getRecords().stream()
                        .map(this::toPurchaseOrderVo)
                        .toList());

        return dashboard;
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




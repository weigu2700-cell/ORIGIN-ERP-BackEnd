package org.smart.erp.production.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 报工完成事件：报工单审批完成（FINISHED）后发布，由成品入库服务监听并自动生成成品入库单。
 * 用以解耦生产报工与成品入库两个服务，避免 Spring Bean 循环依赖。
 */
@Getter
public class ProductionReportFinishedEvent extends ApplicationEvent {

    private final Long productionOrderId;
    private final Long productionReportId;
    private final Long materialId;
    private final Long warehouseId;
    private final BigDecimal qualifiedQuantity;
    private final Long warehousingUserId;

    public ProductionReportFinishedEvent(
            Object source,
            Long productionOrderId,
            Long productionReportId,
            Long materialId,
            Long warehouseId,
            BigDecimal qualifiedQuantity,
            Long warehousingUserId
    ) {
        super(source);
        this.productionOrderId = productionOrderId;
        this.productionReportId = productionReportId;
        this.materialId = materialId;
        this.warehouseId = warehouseId;
        this.qualifiedQuantity = qualifiedQuantity;
        this.warehousingUserId = warehousingUserId;
    }
}

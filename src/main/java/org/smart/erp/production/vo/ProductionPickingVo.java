package org.smart.erp.production.vo;

import lombok.Data;
import org.smart.erp.production.enums.ProductionPickingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductionPickingVo {

    private Long id;

    /** 领料单号 */
    private String pickingNo;

    /** 关联生产订单 ID */
    private Long productionOrderId;

    /** 生产订单号 */
    private String productionOrderNo;

    /** 关联采购需求 ID */
    private Long purchaseDemandId;

    /** 采购需求单号 */
    private String purchaseDemandNo;

    /** 领料物料 ID */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 领料仓库 ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 计划领料数量 */
    private BigDecimal plannedQuantity;

    /** 实际领料数量 */
    private BigDecimal actualQuantity;

    /** 领料单状态 */
    private ProductionPickingStatus status;

    /** 领料时间 */
    private LocalDateTime pickingTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

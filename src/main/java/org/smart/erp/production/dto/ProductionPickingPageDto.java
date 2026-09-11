package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.ProductionPickingStatus;

import java.time.LocalDateTime;

@Data
public class ProductionPickingPageDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 关联生产订单 ID */
    private Long productionOrderId;

    /** 关联采购需求 ID */
    private Long purchaseDemandId;

    /** 领料物料 ID */
    private Long materialId;

    /** 领料仓库 ID */
    private Long warehouseId;

    /** 领料单状态 */
    private ProductionPickingStatus status;

    /** 领料时间起 */
    private LocalDateTime pickingTimeStart;

    /** 领料时间止 */
    private LocalDateTime pickingTimeEnd;
}

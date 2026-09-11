package org.smart.erp.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductionPickingAddDto {

    /** 关联生产订单 ID */
    @NotNull(message = "生产订单不能为空")
    private Long productionOrderId;

    /** 领料物料 ID */
    @NotNull(message = "领料物料不能为空")
    private Long materialId;

    /** 领料仓库 ID */
    @NotNull(message = "领料仓库不能为空")
    private Long warehouseId;

    /** 计划领料数量 */
    @NotNull(message = "计划领料数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "计划领料数量必须大于0")
    private BigDecimal plannedQuantity;

    /** 实际领料数量（可后续回填，默认 0） */
    private BigDecimal actualQuantity;

    /** 领料时间（不填则默认当前时间） */
    private LocalDateTime pickingTime;
}

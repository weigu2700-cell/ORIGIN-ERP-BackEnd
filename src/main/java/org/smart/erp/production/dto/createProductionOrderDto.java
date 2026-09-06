package org.smart.erp.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class createProductionOrderDto {

    /** 关联生产需求 ID（非必填，可由需求自动生成订单） */
    private Long productionDemandId;

    @NotNull(message = "生产物料不能为空")
    private Long materialId;

    @NotNull(message = "计划数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "计划数量必须大于0")
    private BigDecimal plannedQuantity;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private String remark;
}

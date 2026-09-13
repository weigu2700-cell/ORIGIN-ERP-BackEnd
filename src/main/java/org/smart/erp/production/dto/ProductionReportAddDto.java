package org.smart.erp.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductionReportAddDto {

    @NotNull(message = "生产订单ID不能为空")
    private Long productionOrderId;

    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    @NotNull(message = "成品仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "报工数量不能为空")
    @Min(value = 0, message = "报工数量不能为负数")
    private BigDecimal reportQuantity;

    @NotNull(message = "合格数量不能为空")
    @Min(value = 0, message = "合格数量不能为负数")
    private BigDecimal qualifiedQuantity;

    @NotNull(message = "报废数量不能为空")
    @Min(value = 0, message = "报废数量不能为负数")
    private BigDecimal scrappedQuantity;

    private LocalDateTime reportTime;

    private String remark;
}

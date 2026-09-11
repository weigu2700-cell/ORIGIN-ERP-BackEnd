package org.smart.erp.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import org.smart.erp.production.enums.ProductionSourceType;

import java.math.BigDecimal;

@Data
public class ProductionDemandAddDto {

    @NotNull(message = "生产物料不能为空")
    private Long materialId;

    @NotNull(message = "需求数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "需求数量必须大于0")
    private BigDecimal quantity;

    /** 需求来源类型（如销售订单），非必填 */
    private ProductionSourceType sourceType;

    /** 来源单据号（如销售订单号），非必填 */
    private String sourceNo;
}

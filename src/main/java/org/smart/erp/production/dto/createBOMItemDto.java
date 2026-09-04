package org.smart.erp.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class createBOMItemDto {

    @NotNull(message = "BOM不能为空")
    private Long bomId;

    @NotNull(message = "行号不能为空")
    private Integer lineNo;

    @NotNull(message = "物料不能为空")
    private Long componentMaterialId;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal lossRate;

    private String remark;
}

package org.smart.erp.sales.dto.salesDeliveryItemDto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateItemDto {

    @NotNull(message = "销售订单明细ID不能为空")
    private Long salesOrderItemId;

    @NotNull(message = "出库数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "出库数量必须大于0")
    private BigDecimal quantity;

}

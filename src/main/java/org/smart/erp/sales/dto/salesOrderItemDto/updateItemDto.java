package org.smart.erp.sales.dto.salesOrderItemDto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class updateItemDto {

    /**
     * 明细行 id。
     * 有值 -> 更新该行；为 null -> 视为新增行（服务端分配行号并插入）。
     */
    private Long id;

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal unitPrice;
}

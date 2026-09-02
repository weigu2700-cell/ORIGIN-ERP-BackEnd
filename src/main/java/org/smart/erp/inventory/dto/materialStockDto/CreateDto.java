package org.smart.erp.inventory.dto.materialStockDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDto {

    @NotNull(message = "物料id不能为空")
    private Long materialId;

    @NotNull(message = "仓库id不能为空")
    private Long warehouseId;
}

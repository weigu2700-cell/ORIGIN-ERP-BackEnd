package org.smart.erp.inventory.dto.materialStockDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDto {

    private Long materialId;

    private Long warehouseId;

}

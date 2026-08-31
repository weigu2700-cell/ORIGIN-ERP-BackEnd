package org.smart.erp.inventory.dto.transactionDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDto {

    private Long materialId;

    private Long warehouseId;

    private BigDecimal quantity;

    private String remark;
}

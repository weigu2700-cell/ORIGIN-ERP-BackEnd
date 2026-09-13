package org.smart.erp.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionAddDto {

    private Long materialId;

    private Long warehouseId;

    private BigDecimal quantity;

    private String remark;
}

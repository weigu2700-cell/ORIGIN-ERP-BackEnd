package org.smart.erp.inventory.dto;

import lombok.Data;

@Data
public class MaterialStockPageDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long materialId;

    private Long warehouseId;

    private String materialCode;
}

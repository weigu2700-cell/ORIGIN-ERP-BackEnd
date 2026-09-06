package org.smart.erp.inventory.dto.materialStockDto;

import lombok.Data;

@Data
public class ListDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long materialId;

    private Long warehouseId;

    private String materialCode;
}

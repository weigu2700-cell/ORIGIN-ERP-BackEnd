package org.smart.erp.inventory.dto.materialStockDto;

import lombok.Data;

@Data
public class ListDto {

    private Integer pageNum;

    private Integer pageSize;

    private Long materialId;

    private Long warehouseId;

    private String materialCode;

}

package org.smart.erp.inventory.dto.transactionDto;

import lombok.Data;

@Data
public class ListDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long materialId;

    private Long warehouseId;

    private String businessType;

    private String businessNo;
}

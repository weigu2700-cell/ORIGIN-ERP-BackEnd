package org.smart.erp.sales.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderItemVo {

    private Long id;

    private Long salesOrderId;

    private Long materialId;

    private String materialName;

    private Long warehouseId;

    private String warehouseName;

    private Integer quantity;

    private BigDecimal unitPrice;
}

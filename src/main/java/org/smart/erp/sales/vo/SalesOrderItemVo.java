package org.smart.erp.sales.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderItemVo {

    private Long id;

    private Integer lineNo;

    private Long salesOrderId;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private Long warehouseId;

    private String warehouseName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;
}

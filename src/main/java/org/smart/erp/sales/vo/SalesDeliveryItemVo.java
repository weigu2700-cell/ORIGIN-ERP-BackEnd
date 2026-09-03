package org.smart.erp.sales.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesDeliveryItemVo {

    private Long id;

    private Long deliveryId;

    private Integer lineNo;

    private Long salesOrderItemId;

    private Long materialId;

    private String materialName;

    private String materialCode;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal quantity;
}

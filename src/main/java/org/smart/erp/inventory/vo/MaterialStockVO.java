package org.smart.erp.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialStockVO {

    private Long id;

    private Long materialId;

    private String materialName;

    private String materialCode;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal onHand;

    private BigDecimal reserved;

    private BigDecimal available;


}

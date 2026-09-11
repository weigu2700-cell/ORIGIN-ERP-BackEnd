package org.smart.erp.master.dto.MaterialSupplierDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialSupplierAddDto {

    private Long materialId;

    private Long supplierId;

    private String materialSupplierCode;

    private BigDecimal purchasePrice;

    private int leadTimeDays;

    private Integer preferred;

    private BigDecimal minOrderQty;

    private String remark;
}

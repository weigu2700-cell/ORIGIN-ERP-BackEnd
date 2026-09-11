package org.smart.erp.master.dto.MaterialSupplierDto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialSupplierUpdateDto {

    private String materialSupplierCode;

    private BigDecimal purchasePrice;

    private Integer leadTimeDays;

    private Integer preferred;

    private BigDecimal minOrderQty;

    private String remark;
}

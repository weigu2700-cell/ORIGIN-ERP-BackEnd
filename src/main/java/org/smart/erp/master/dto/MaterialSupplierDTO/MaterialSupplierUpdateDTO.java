package org.smart.erp.master.dto.MaterialSupplierDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialSupplierUpdateDTO {

    private String materialSupplierCode;

    private BigDecimal purchasePrice;

    private Integer leadTimeDays;

    private Integer preferred;

    private BigDecimal minOrderQty;

    private String remark;
}

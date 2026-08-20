package org.smart.erp.master.dto.MaterialSupplierDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialSupplierCreateDTO {

    private Long materialId;

    private Long supplierId;

    private String materialSupplierCode;

    private BigDecimal purchasePrice;

    private int leadTimeDays;

    private Integer preferred;

    private BigDecimal minOrderQty;

    private String remark;
}

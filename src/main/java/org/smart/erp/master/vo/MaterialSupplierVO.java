package org.smart.erp.master.vo;

import lombok.Data;
import org.smart.erp.master.enums.MaterialSupplierStatus;

import java.math.BigDecimal;

@Data
public class MaterialSupplierVO {

    private Long id;

    private String materialSupplierCode;

    private String materialName;

    private String supplierName;

    private BigDecimal purchasePrice;

    private int leadTimeDays;

    private int preferred;

    private BigDecimal minOrderQty;

    private String remark;

    private MaterialSupplierStatus status;
}

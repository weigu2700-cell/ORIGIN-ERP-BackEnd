package org.smart.erp.master.vo.ExcelPrintVo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialSupplierExportVo {

    @ExcelIgnore
    private Long id;

    @ExcelProperty("物料供应商编码")
    private String materialSupplierCode;

    @ExcelProperty("物料名称")
    private String materialName;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("采购单价")
    private BigDecimal purchasePrice;

    @ExcelProperty("交期(天)")
    private int leadTimeDays;

    @ExcelProperty("是否首选")
    private String preferredDesc;

    @ExcelProperty("最小订货量")
    private BigDecimal minOrderQty;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("备注")
    private String remark;
}

package org.smart.erp.master.vo.ExcelPrintVo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialExportVO {

    @ExcelIgnore
    private Long id;

    @ExcelProperty("物料编码")
    private String code;

    @ExcelProperty("物料名称")
    private String name;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("物料类型")
    private String typeDesc;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("安全库存")
    private BigDecimal safetyStock;

    @ExcelProperty("备注")
    private String remark;
}

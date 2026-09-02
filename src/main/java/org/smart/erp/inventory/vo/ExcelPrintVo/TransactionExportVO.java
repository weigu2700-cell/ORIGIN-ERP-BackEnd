package org.smart.erp.inventory.vo.ExcelPrintVo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionExportVO {

    @ExcelIgnore
    private Long id;

    @ExcelProperty(value = "物料编码")
    private String materialCode;

    @ExcelProperty(value = "物料名称")
    private String materialName;

    @ExcelProperty(value = "仓库名称")
    private String warehouseName;

    @ExcelProperty(value = "流水类型")
    private String transactionTypeName;

    @ExcelProperty(value = "业务类型")
    private String businessType;

    @ExcelProperty(value = "业务单号")
    private String businessNo;

    @ExcelProperty(value = "数量")
    private BigDecimal quantity;

    @ExcelProperty(value = "变动前库存")
    private BigDecimal beforeOnHand;

    @ExcelProperty(value = "变动后库存")
    private BigDecimal afterOnHand;

    @ExcelProperty(value = "变动前预占")
    private BigDecimal beforeReserved;

    @ExcelProperty(value = "变动后预占")
    private BigDecimal afterReserved;

    @ExcelProperty(value = "备注")
    private String remark;

    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;
}

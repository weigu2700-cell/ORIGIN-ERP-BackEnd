package org.smart.erp.master.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductionLineExportVo {

    @ExcelIgnore
    private Long id;

    @ExcelProperty("生产线编码")
    private String code;

    @ExcelProperty("生产线名称")
    private String name;

    @ExcelProperty("所属车间")
    private String workshopName;

    @ExcelProperty("日产能")
    private BigDecimal capacityPerDay;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}

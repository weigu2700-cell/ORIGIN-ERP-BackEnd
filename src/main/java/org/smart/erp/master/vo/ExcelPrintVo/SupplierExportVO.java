package org.smart.erp.master.vo.ExcelPrintVo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierExportVo {

    @ExcelIgnore
    private Long id;

    @ExcelProperty("供应商编码")
    private String code;

    @ExcelProperty("供应商名称")
    private String name;

    @ExcelProperty("供应商简称")
    private String shortName;

    @ExcelProperty("联系人")
    private String contactName;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("地址")
    private String address;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}

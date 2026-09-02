package org.smart.erp.inventory.dto.ExcelPrintDto;

import cn.idev.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存流水 Excel 导入模板。
 * 表头使用业务标识（物料编码/仓库名称）而非内部主键，导入时由 service 反查 id。
 * 流水类型列取值：入库 / 预占 / 释放预占 / 出库。
 */
@Data
public class TransactionImportDto {

    @ExcelProperty(value = "物料编码")
    @NotBlank(message = "物料编码不能为空")
    private String materialCode;

    @ExcelProperty(value = "物料名称")
    private String materialName;

    @ExcelProperty(value = "仓库名称")
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    @ExcelProperty(value = "流水类型")
    @NotBlank(message = "流水类型不能为空")
    private String transactionTypeName;

    @ExcelProperty(value = "业务类型")
    private String businessType;

    @ExcelProperty(value = "数量")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于 0")
    private BigDecimal quantity;

    @ExcelProperty(value = "备注")
    private String remark;
}

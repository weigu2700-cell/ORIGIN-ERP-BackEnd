package org.smart.erp.production.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.production.enums.ProductionReportStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "prd_production_report")
public class ProductionReport {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String productionReportNo;

    private Long productionOrderId;

    private Long materialId;

    /** 成品入库仓库（报工时指定，用于自动生成成品入库单） */
    private Long warehouseId;

    private BigDecimal reportQuantity;

    private BigDecimal qualifiedQuantity;

    private BigDecimal scrappedQuantity;

    private ProductionReportStatus status;

    @DateTimeFormat(pattern = "yyy-mm-dd HH:MM:SS")
    private LocalDateTime reportTime;

    private Long reportUserId;

    private String remark;

    @DateTimeFormat(pattern = "yyy-mm-dd HH:MM:SS")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyy-mm-dd HH:MM:SS")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}

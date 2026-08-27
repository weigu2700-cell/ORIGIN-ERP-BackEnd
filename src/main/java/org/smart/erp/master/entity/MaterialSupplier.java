package org.smart.erp.master.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.master.enums.MaterialSupplierStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("md_material_supplier")
public class MaterialSupplier {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long materialId;

    private Long supplierId;

    private String materialSupplierCode;

    private BigDecimal purchasePrice;

    private int leadTimeDays;

    private Integer preferred;

    private BigDecimal minOrderQty;

    private MaterialSupplierStatus status;

    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private int deleted = 0;
}

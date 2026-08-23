package org.smart.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_material_stock")
public class MaterialStock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal onHand;

    private BigDecimal reserved;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private int version;
}

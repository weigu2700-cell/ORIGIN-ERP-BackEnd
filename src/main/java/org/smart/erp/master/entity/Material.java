package org.smart.erp.master.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.enums.MaterialType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("md_material")
public class Material {

    @TableId(value = "id" , type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String name;

    private String spec;

    private BigDecimal safetyStock;

    private MaterialType type;

    private String unit;

    private MaterialStatus status;

    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time" , fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time" , fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private int deleted = 0;
}


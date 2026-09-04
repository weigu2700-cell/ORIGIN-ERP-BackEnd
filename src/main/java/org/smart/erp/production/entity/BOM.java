package org.smart.erp.production.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.smart.erp.production.enums.BOMStatus;

import java.time.LocalDateTime;

@Data
@TableName("prd_bom")
public class BOM {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String bomNo;

    private Long materialId;

    private BOMStatus status;

    @Version
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

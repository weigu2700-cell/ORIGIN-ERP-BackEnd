package org.smart.erp.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.system.Enum.RoleEnum;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class RoleInfo {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String code;

    private int sort ;

    private RoleEnum status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private int deleted = 0;
}

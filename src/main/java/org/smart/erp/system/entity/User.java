package org.smart.erp.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.system.Enum.UserStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class User {

    @TableId(value = "id" , type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String password;

    private Long deptId;

    private String phone;

    private UserStatus status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private int deleted = 0;

}

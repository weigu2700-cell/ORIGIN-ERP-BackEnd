package org.smart.erp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_menu")
public class RoleMenu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private Long menuId;
}

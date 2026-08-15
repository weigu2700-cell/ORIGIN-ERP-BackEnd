package org.smart.erp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class RolePermission {

    @TableId(value="id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private Long permissionId;
}

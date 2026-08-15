package org.smart.erp.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色-权限分配请求。
 * 前端传入角色 id 与要绑定的权限 id 列表，
 * 后端以"全量覆盖"方式写入 sys_role_permission 对照表。
 */
@Data
public class RolePermissionAssignDTO {

    /** 角色 id（查的角色，不写入关联表） */
    private Long roleId;

    /** 要分配给该角色的权限 id 列表（查/选都由前端决定，这里只负责写入） */
    private List<Long> permissionIds;
}

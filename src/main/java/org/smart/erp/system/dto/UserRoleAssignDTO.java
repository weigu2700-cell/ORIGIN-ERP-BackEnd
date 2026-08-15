package org.smart.erp.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户-角色分配请求。
 * 前端传入用户 id 与要绑定的角色 id 列表，
 * 后端以"全量覆盖"方式写入 sys_user_role 对照表。
 */
@Data
public class UserRoleAssignDTO {

    /** 用户 id（查的用户，不写入关联表） */
    private Long userId;

    /** 要分配给该用户的角色 id 列表（查/选都由前端决定，这里只负责写入） */
    private List<Long> roleIds;
}

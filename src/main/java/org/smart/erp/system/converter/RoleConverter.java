package org.smart.erp.system.converter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.smart.erp.system.entity.UserRole;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.Enum.RoleEnum;
import org.smart.erp.system.mapper.UserRoleMapper;
import org.smart.erp.system.mapper.RoleInfoMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleConverter {

    private final UserRoleMapper userRoleMapper;
    private final RoleInfoMapper roleInfoMapper;

    public RoleConverter(UserRoleMapper userRoleMapper, RoleInfoMapper roleInfoMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleInfoMapper = roleInfoMapper;
    }

    /** 获取用户当前的角色ID列表 */
    public List<Long> getCurrentRoleIds(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery()
                                .eq(UserRole::getUserId, userId)
                )
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleInfoMapper.selectList(Wrappers.<RoleInfo>lambdaQuery()
                        .in(RoleInfo::getId, roleIds)
                        .eq(RoleInfo::getStatus, RoleEnum.ENABLE))
                .stream()
                .map(RoleInfo::getId)
                .toList();
    }

    /** 超级管理员由角色编码识别，避免依赖固定角色 ID。 */
    public boolean isSuperAdmin(Long userId) {
        List<Long> roleIds = getCurrentRoleIds(userId);
        return !roleIds.isEmpty() && roleInfoMapper.selectCount(
                Wrappers.<RoleInfo>lambdaQuery().in(RoleInfo::getId, roleIds).eq(RoleInfo::getCode, "admin")) > 0;
    }
}

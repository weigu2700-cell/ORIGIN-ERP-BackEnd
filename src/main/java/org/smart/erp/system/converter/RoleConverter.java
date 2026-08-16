package org.smart.erp.system.converter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.smart.erp.system.entity.UserRole;
import org.smart.erp.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleConverter {

    private final UserRoleMapper userRoleMapper;

    public RoleConverter(UserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    /** 获取用户当前的角色ID列表 */
    public List<Long> getCurrentRoleIds(Long userId) {
        return userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery()
                                .eq(UserRole::getUserId, userId)
                )
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
    }
}

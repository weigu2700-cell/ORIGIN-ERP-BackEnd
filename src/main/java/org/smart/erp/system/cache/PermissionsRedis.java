package org.smart.erp.system.cache;

import org.smart.erp.common.utils.redis.OperationSet;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionCacheVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionsRedis {

    private final String PERMISSIONS_KEY_PREFIX = "erp:auth:permissions:";
    private final String PERMISSIONS_USERS_INDEX = "erp:auth:permissions:users";

    private final OperationString operationString;
    private final OperationSet operationSet;

    public PermissionsRedis(OperationString operationString, OperationSet operationSet) {
        this.operationString = operationString;
        this.operationSet = operationSet;
    }

    public Set<PermissionCacheVo> getPermissionsCache(Long userId) {
        if (!operationString.hasKey(PERMISSIONS_KEY_PREFIX, userId)) {
            return null;
        }
        return operationSet.members(PERMISSIONS_KEY_PREFIX, userId);
    }

    public void activePermissionsCache(Long userId, Set<PermissionCacheVo> permissions) {
        operationSet.addAll(PERMISSIONS_KEY_PREFIX, userId, permissions, Duration.ofHours(2));
        operationSet.add(PERMISSIONS_USERS_INDEX, userId, Duration.ofHours(2));
    }

    public void evictPermissionsCache(Long userId) {
        operationString.delete(PERMISSIONS_KEY_PREFIX, userId);
        operationSet.remove(PERMISSIONS_USERS_INDEX, userId);
    }

    public void evictAllPermissionsCache() {
        Set<Long> userIds = operationSet.members(PERMISSIONS_USERS_INDEX);
        if (userIds != null) {
            for (Long id : userIds) {
                operationString.delete(PERMISSIONS_KEY_PREFIX, id);
            }
        }
        operationSet.delete(PERMISSIONS_USERS_INDEX);
    }

    public Set<PermissionCacheVo> buildPermissionCache(List<Permission> permissions) {
        return permissions.stream().map(permission -> {
            PermissionCacheVo permissionCacheVo = new PermissionCacheVo();
            BeanUtils.copyProperties(permission, permissionCacheVo);
            return permissionCacheVo;
        }).collect(Collectors.toSet());
    }
}

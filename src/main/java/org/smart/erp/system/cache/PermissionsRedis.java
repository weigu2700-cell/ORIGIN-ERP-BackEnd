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

    private final String PERMISSIONS_KEY_PREFIX = "erp:auth:permissions:v2:";
    private final String PERMISSIONS_USERS_INDEX = "erp:auth:permissions:v2:users";

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
        Set<Object> userIds = operationSet.members(PERMISSIONS_USERS_INDEX);
        if (userIds != null) {
            for (Object rawId : userIds) {
                Long userId = toLong(rawId);
                if (userId != null) {
                    operationString.delete(PERMISSIONS_KEY_PREFIX, userId);
                }
            }
        }
        operationSet.delete(PERMISSIONS_USERS_INDEX);
    }

    /**
     * 把 Redis 取出的成员安全转成 Long。
     * 正常情况下成员是 Long；若因历史数据/旧序列化器残留为 String，也能正确解析，
     * 避免 {@code for (Long id : ...)} 在运行时把 String 当 Long 强转抛出 ClassCastException。
     */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public Set<PermissionCacheVo> buildPermissionCache(List<Permission> permissions) {
        return permissions.stream().map(permission -> {
            PermissionCacheVo permissionCacheVo = new PermissionCacheVo();
            BeanUtils.copyProperties(permission, permissionCacheVo);
            return permissionCacheVo;
        }).collect(Collectors.toSet());
    }
}

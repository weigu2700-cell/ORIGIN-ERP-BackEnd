package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.system.cache.PermissionsRedis;
import org.smart.erp.system.converter.RoleConverter;
import org.smart.erp.system.dto.PermissionAddDto;
import org.smart.erp.system.dto.PermissionDetailDto;
import org.smart.erp.system.dto.PermissionUpdateDto;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.entity.RolePermission;
import org.smart.erp.system.Enum.Status;
import org.smart.erp.system.mapper.PermissionMapper;
import org.smart.erp.system.mapper.RolePermissionMapper;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionCacheVo;
import org.smart.erp.system.vo.PermissionTreeVo;
import org.smart.erp.system.vo.PermissionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {


    private final CurrentUser currentUser;
    private final RoleConverter roleConverter;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionsRedis permissionsRedis;

    public PermissionServiceImpl(
            CurrentUser currentUser,
            RoleConverter roleConverter,
            RolePermissionMapper rolePermissionMapper,
            PermissionsRedis permissionsRedis
    ) {
        this.currentUser = currentUser;
        this.roleConverter = roleConverter;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionsRedis = permissionsRedis;
    }


    /**
     * 权限缓存读取（旁路缓存）。
     * 先按权限 id 查 Redis，未命中则回源数据库并回填缓存。
     * @param permissionId 权限 id
     * @return 权限缓存对象；权限不存在时返回 null
     */
    private PermissionCacheVo activePermissionWithCache(Long permissionId) {
        PermissionCacheVo permissionCacheVo = permissionsRedis.getPermissionCache(permissionId);
        if (permissionCacheVo != null) {
            return permissionCacheVo;
        }
        Permission entity = this.getById(permissionId);
        if (entity == null) {
            return null;
        }
        permissionCacheVo = permissionsRedis.buildPermissionCache(entity);
        permissionsRedis.activePermissionCache(permissionCacheVo);
        return permissionCacheVo;
    }

    /**
     * 将权限实体转换为 Vo。
     * parentNameById 为父级 id -> 父级名称 的映射，可由调用方批量查询后传入；
     * 不需要父级名称时传空 Map 即可（parentName 置为 null）。
     * @param p 权限实体
     * @param parentNameById 父级 id -> 父级名称 的映射
     * @return 权限 Vo
     */
    private PermissionTreeVo toVO(Permission p, Map<Long, String> parentNameById) {
        PermissionTreeVo vo = new PermissionTreeVo();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setType( p.getType());
        vo.setParentId(p.getParentId());
        vo.setParentName(p.getParentId() != null ? parentNameById.get(p.getParentId()) : null);
        vo.setSort(p.getSort());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        return vo;
    }

    // 获取当前用户的权限
    @Override
    public List<PermissionVo> detailCurrentUserPermission(Long currentUserId) {
        List<Long> roleIds = roleConverter.getCurrentRoleIds(currentUserId);
        // 用户没有角色时直接返回空权限：in() 传入空集合会拼出 "IN ()"，导致 SQL 语法错误
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<RolePermission> rolePermissions =
                rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>()
                                .in(RolePermission::getRoleId, roleIds)
                );
        // 角色没有配置任何权限时同样短路，避免 listByIds 拼出空 IN
        if (rolePermissions.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();

        // 优先走缓存；缓存层异常（如 Redis 未启动）时整体回退数据库，
        // 避免认证链路因缓存故障直接 401 把用户踢回登录页
        Map<Long, PermissionCacheVo> permissionMap = loadPermissionMap(permissionIds);

        List<PermissionVo> vos = new ArrayList<>();
        for (RolePermission rolePermission : rolePermissions) {
            PermissionCacheVo cacheVo = permissionMap.get(rolePermission.getPermissionId());
            if (cacheVo != null) {
                PermissionVo vo = new PermissionVo();
                BeanUtils.copyProperties(cacheVo, vo);
                vos.add(vo);
            }
        }
        return vos;
    }

    /**
     * 读取权限映射。缓存可用时逐条走旁路缓存；缓存不可用时回退数据库，
     * 保证登录 / 鉴权链路不依赖 Redis 存活。
     */
    private Map<Long, PermissionCacheVo> loadPermissionMap(List<Long> permissionIds) {
        try {
            Map<Long, PermissionCacheVo> cached = permissionIds.stream()
                    .map(this::activePermissionWithCache)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(PermissionCacheVo::getId, vo -> vo));
            if (cached.size() == permissionIds.size()) {
                return cached;
            }
            // 部分未命中（如数据库已无该权限）时用数据库补齐，避免漏权限
            return fillFromDb(permissionIds, cached);
        } catch (Exception ex) {
            // Redis 未启动 / 网络异常等缓存故障：直接走数据库兜底
            return fillFromDb(permissionIds, new java.util.HashMap<>());
        }
    }

    private Map<Long, PermissionCacheVo> fillFromDb(
            List<Long> permissionIds,
            Map<Long, PermissionCacheVo> base
    ) {
        if (permissionIds.isEmpty()) {
            return base;
        }
        this.listByIds(permissionIds).forEach(p -> {
            PermissionCacheVo vo = new PermissionCacheVo();
            BeanUtils.copyProperties(p, vo);
            base.putIfAbsent(p.getId(), vo);
        });
        return base;
    }

    @Override
    public Page<PermissionTreeVo> pagePermission(PermissionDetailDto dto) {
        LambdaQueryWrapper<Permission> queryWrapper =
                new LambdaQueryWrapper<Permission>()
                        .like(dto.getName() != null, Permission::getName, dto.getName())
                        .like(dto.getCode() != null, Permission::getCode, dto.getCode())
                        .eq(dto.getType() != null, Permission::getType, dto.getType())
                        .eq(dto.getStatus() != null, Permission::getStatus, dto.getStatus())
                        .eq(dto.getParentId() != null, Permission::getParentId, dto.getParentId())
                        .orderByDesc(Permission::getSort);

        Page<Permission> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), queryWrapper);

        // 批量解析父级权限名称，避免逐条查询（N+1）
        List<Long> parentIds = page.getRecords().stream()
                .map(Permission::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> parentNameById = parentIds.isEmpty() ? Map.of() :
                this.list(new LambdaQueryWrapper<Permission>().in(Permission::getId, parentIds))
                        .stream()
                        .collect(Collectors.toMap(Permission::getId, Permission::getName));

        // 用统一转换方法把实体转成 Vo，并带上父级名称
        List<PermissionTreeVo> vos = page.getRecords().stream()
                .map(p -> toVO(p, parentNameById))
                .toList();

        Page<PermissionTreeVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    /**
     * 查询权限树（父子层级结构）。
     * 思路：
     * 1. 一次性查出所有未删除的权限（权限表通常数据量不大，全量查即可，避免递归 SQL）。
     * 2. 先全部转成 Vo，并建立 "id -> Vo" 的索引，方便子节点快速挂到父节点下。
     * 3. 遍历每个节点：
     *    - 若 parentId 为空或为 0，说明是顶级节点，放入树的根列表；
     *    - 否则从索引里找到父节点，把自己加进父节点的 children 列表。
     * 4. 按 sort 倒序（与列表接口保持一致），让前端展示有序。
     * 时间复杂度 O(n)，只查一次数据库，没有 N+1 问题。
     */
    @Override
    public List<PermissionTreeVo> getPermissionTree() {
        List<Permission> all = this.list(new LambdaQueryWrapper<Permission>().orderByDesc(Permission::getSort));

        Map<Long, PermissionTreeVo> voById = all.stream()
                .map(p -> toVO(p, Map.of()))
                .collect(Collectors.toMap(PermissionTreeVo::getId, vo -> vo));

        List<PermissionTreeVo> tree = new java.util.ArrayList<>();
        for (PermissionTreeVo node : voById.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                tree.add(node);
            } else {
                PermissionTreeVo parent = voById.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new java.util.ArrayList<>());
                    }
                    parent.getChildren().add(node);
                } else {
                    tree.add(node);
                }
            }
        }
        return tree;
    }

    @Override
    public PermissionTreeVo detailPermission(Long id) {
        Permission permission = this.getById(id);
        if (permission == null) {
            throw new BusinessException(404, "权限不存在");
        }
        Map<Long, String> parentNameById = Map.of();
        if (permission.getParentId() != null) {
            Permission parent = this.getById(permission.getParentId());
            if (parent != null) {
                parentNameById = Map.of(permission.getParentId(), parent.getName());
            }
        }
        return toVO(permission, parentNameById);
    }

    @Override
    public void addPermission(PermissionAddDto dto) {
        if (dto.getParentId() != null) {
            Permission parent = this.getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父权限不存在");
            }
        }
        if (this.count(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, dto.getCode())) > 0) {
            throw new BusinessException(400, "权限编码已存在");
        }
        Permission permission = new Permission();
        BeanUtils.copyProperties(dto, permission);
        if (permission.getStatus() == null) {
            permission.setStatus(Status.ENABLE);
        }
        this.save(permission);
        permissionsRedis.evictPermissionCache(permission.getId());
    }

    @Override
    @Transactional
    public void updatePermission(PermissionUpdateDto dto) {
        Permission permission = this.getById(dto.getId());
        if (permission == null) {
            throw new BusinessException(404, "权限不存在");
        }
        if (dto.getParentId() != null) {
            Permission parent = this.getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父权限不存在");
            }
        }
        if (dto.getCode() != null) {
            if (!dto.getCode().equals(permission.getCode())
                    && this.count(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, dto.getCode())) > 0) {
                throw new BusinessException(400, "权限编码已存在");
            }
        }
        if (dto.getCode() != null) permission.setCode(dto.getCode());
        if (dto.getName() != null) permission.setName(dto.getName());
        if (dto.getType() != null) permission.setType(dto.getType());
        if (dto.getParentId() != null) permission.setParentId(dto.getParentId());
        if (dto.getStatus() != null) permission.setStatus(dto.getStatus());
        if (dto.getRemark() != null) permission.setRemark(dto.getRemark());

        this.updateById(permission);
        // 写后失效，避免缓存残留旧数据（TTL 2h 兜底）
        permissionsRedis.evictPermissionCache(permission.getId());
    }

    @Override
    public List<PermissionVo> getCurrentUserPermission() {
        Long currentUserId = currentUser.getUserId();
       return detailCurrentUserPermission(currentUserId);

    }


}

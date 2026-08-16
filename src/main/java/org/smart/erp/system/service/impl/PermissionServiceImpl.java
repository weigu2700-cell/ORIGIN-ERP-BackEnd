package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.system.dto.PermissionCreateDTO;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.mapper.PermissionMapper;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionTreeVO;
import org.smart.erp.system.vo.PermissionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {


    /**
     * 将权限实体转换为 VO。
     * parentNameById 为父级 id -> 父级名称 的映射，可由调用方批量查询后传入；
     * 不需要父级名称时传空 Map 即可（parentName 置为 null）。
     */
    private PermissionTreeVO toVO(Permission p, Map<Long, String> parentNameById) {
        PermissionTreeVO vo = new PermissionTreeVO();
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

    @Override
    public Page<PermissionTreeVO> getPermissionList(PermissionGetDTO dto) {
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(dto.getName() != null, Permission::getName, dto.getName());
        queryWrapper.like(dto.getCode() != null, Permission::getCode, dto.getCode());
        queryWrapper.eq(dto.getType() != null, Permission::getType, dto.getType());
        queryWrapper.eq(dto.getStatus() != null, Permission::getStatus, dto.getStatus());
        queryWrapper.eq(dto.getParentId() != null, Permission::getParentId, dto.getParentId());
        queryWrapper.orderByDesc(Permission::getSort);

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

        // 用统一转换方法把实体转成 VO，并带上父级名称
        List<PermissionTreeVO> vos = page.getRecords().stream()
                .map(p -> toVO(p, parentNameById))
                .toList();

        Page<PermissionTreeVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    /**
     * 查询权限树（父子层级结构）。
     *
     * 思路：
     * 1. 一次性查出所有未删除的权限（权限表通常数据量不大，全量查即可，避免递归 SQL）。
     * 2. 先全部转成 VO，并建立 "id -> VO" 的索引，方便子节点快速挂到父节点下。
     * 3. 遍历每个节点：
     *    - 若 parentId 为空或为 0，说明是顶级节点，放入树的根列表；
     *    - 否则从索引里找到父节点，把自己加进父节点的 children 列表。
     * 4. 按 sort 倒序（与列表接口保持一致），让前端展示有序。
     *
     * 时间复杂度 O(n)，只查一次数据库，没有 N+1 问题。
     */
    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        List<Permission> all = this.list(new LambdaQueryWrapper<Permission>().orderByDesc(Permission::getSort));

        Map<Long, PermissionTreeVO> voById = all.stream()
                .map(p -> toVO(p, Map.of()))
                .collect(Collectors.toMap(PermissionTreeVO::getId, vo -> vo));

        List<PermissionTreeVO> tree = new java.util.ArrayList<>();
        for (PermissionTreeVO node : voById.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                tree.add(node);
            } else {
                PermissionTreeVO parent = voById.get(parentId);
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
    public PermissionTreeVO getPermissionDetail(Long id) {
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
    public void createPermission(PermissionCreateDTO dto) {
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
        this.save(permission);
    }


}

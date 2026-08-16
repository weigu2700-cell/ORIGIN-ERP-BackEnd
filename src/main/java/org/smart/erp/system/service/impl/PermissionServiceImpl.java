package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.mapper.PermissionMapper;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionVO;
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
    private PermissionVO toVO(Permission p, Map<Long, String> parentNameById) {
        PermissionVO vo = new PermissionVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setType(p.getType());
        vo.setParentId(p.getParentId());
        vo.setParentName(p.getParentId() != null ? parentNameById.get(p.getParentId()) : null);
        vo.setSort(p.getSort());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        return vo;
    }

    @Override
    public Page<PermissionVO> getPermissionList(PermissionGetDTO dto) {
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
        List<PermissionVO> vos = page.getRecords().stream()
                .map(p -> toVO(p, parentNameById))
                .toList();

        Page<PermissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
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
    public List<PermissionVO> getPermissionTree() {
        // 1. 查询全部权限（逻辑删除字段 deleted 由 MyBatis-Plus 自动过滤）
        List<Permission> all = this.list(new LambdaQueryWrapper<Permission>().orderByDesc(Permission::getSort));

        // 2. 转成 VO 并建立 id 索引；同时准备一个 parentId -> 子节点列表 的临时容器
        Map<Long, PermissionVO> voById = all.stream()
                .map(p -> toVO(p, Map.of()))   // 树结构不需要 parentName，传空 Map 即可
                .collect(Collectors.toMap(PermissionVO::getId, vo -> vo));

        // 3. 组装树：把每个节点挂到对应父节点下
        List<PermissionVO> tree = new java.util.ArrayList<>();
        for (PermissionVO node : voById.values()) {
            Long parentId = node.getParentId();
            // 顶级节点：parentId 为 null 或 0
            if (parentId == null || parentId == 0L) {
                tree.add(node);
            } else {
                PermissionVO parent = voById.get(parentId);
                if (parent != null) {
                    // 父节点存在则挂到其 children；不存在（脏数据）则按顶级处理，避免丢失
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


}

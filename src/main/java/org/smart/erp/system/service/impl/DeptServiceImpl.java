package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.system.converter.DeptConverter;
import org.smart.erp.system.dto.DeptDTO;
import org.smart.erp.system.dto.DeptListDTO;
import org.smart.erp.system.dto.DeptUpdateDTO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.mapper.DeptMapper;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.service.DeptService;
import org.smart.erp.system.vo.DeptTreeVO;
import org.smart.erp.system.vo.DeptVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    private final UserMapper userMapper;

    public DeptServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }


    private DeptTreeVO toTreeVO(Dept dept) {
        DeptTreeVO vo = new DeptTreeVO();
        BeanUtils.copyProperties(dept, vo);
        // id/parentId 需要转成字符串，BeanUtils 对 Long -> String 类型不匹配会跳过，需手动设置
        if (dept.getId() != null) {
            vo.setId(String.valueOf(dept.getId()));
        }
        if (dept.getParentId() != null) {
            vo.setParentId(String.valueOf(dept.getParentId()));
        }
        return vo;
    }

    /** 单条转换并填充父部门名 */
    private DeptVO toVOWithParent(Dept dept) {
        String parentName = null;
        if (dept.getParentId() != null) {
            Dept parent = this.getById(dept.getParentId());
            parentName = parent != null ? parent.getName() : null;
        }
        return DeptConverter.toVO(dept, parentName);
    }

    /** 批量查询父部门名，返回 parentId -> name 映射，避免 N+1 */
    private Map<Long, String> parentNameMap(List<Dept> depts) {
        List<Long> parentIds = depts.stream()
                .map(Dept::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return this.listByIds(parentIds).stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(DeptDTO dto) {
        if (this.count(new LambdaQueryWrapper<Dept>().eq(Dept::getName, dto.getName())) > 0) {
            throw new BusinessException(400, "部门名称已存在");
        }
        if (this.count(new LambdaQueryWrapper<Dept>().eq(Dept::getCode, dto.getCode())) > 0) {
            throw new BusinessException(400, "部门编码已存在");
        }
        if (dto.getParentId() != null && this.getById(dto.getParentId()) == null) {
            throw new BusinessException(400, "上级部门不存在");
        }
        Dept dept = new Dept();
        BeanUtils.copyProperties(dto, dept);
        this.save(dept);
    }

    @Override
    public DeptVO getDeptDetail(Long id) {
        Dept dept = this.getById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        return toVOWithParent(dept);
    }

    @Override
    public Page<DeptVO> listDept(DeptListDTO dto) {
        LambdaQueryWrapper<Dept> queryWrapper = new LambdaQueryWrapper<Dept>()
                .like(dto.getName() != null, Dept::getName, dto.getName())
                .like(dto.getCode() != null, Dept::getCode, dto.getCode())
                .eq(dto.getParentId() != null, Dept::getParentId, dto.getParentId())
                .eq(dto.getStatus() != null, Dept::getStatus, dto.getStatus())
                .orderByDesc(Dept::getSort);

        long current = dto.getPage() > 0 ? dto.getPage() : 1;
        long size = dto.getPageSize() > 0 ? dto.getPageSize() : 10;

        long total = this.count(queryWrapper);
        Page<Dept> page = this.page(new Page<>(current, size), queryWrapper);

        // 父部门名一次性批量查出，避免循环里逐条查库（N+1）
        List<Long> parentIds = page.getRecords().stream()
                .map(Dept::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> parentNameMap = parentIds.isEmpty()
                ? Map.of()
                : this.listByIds(parentIds).stream()
                    .collect(Collectors.toMap(Dept::getId, Dept::getName));

        Page<DeptVO> voPage = new Page<>(current, size, total);
        voPage.setRecords(page.getRecords().stream()
                .map(dept -> DeptConverter.toVO(dept, parentNameMap.get(dept.getParentId())))
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptUpdateDTO dto) {
        Dept dept = this.getById(dto.getId());
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(dept.getId())) {
                throw new BusinessException(400, "上级部门不能设置为自己");
            }
            if (this.getById(dto.getParentId()) == null) {
                throw new BusinessException(400, "上级部门不存在");
            }
            // 防止把上级部门设置为自己的子孙，避免树出现循环引用（序列化会栈溢出）
            Long cur = dto.getParentId();
            Set<Long> visited = new HashSet<>();
            while (cur != null) {
                if (cur.equals(dept.getId())) {
                    throw new BusinessException(400, "上级部门不能设置为自己的下级部门");
                }
                if (!visited.add(cur)) {
                    break; // 数据本身已存在环时的防御性退出
                }
                Dept parent = this.getById(cur);
                cur = parent != null ? parent.getParentId() : null;
            }
        }
        if (dto.getName() != null && !dto.getName().equals(dept.getName())
                && this.count(new LambdaQueryWrapper<Dept>()
                    .eq(Dept::getName, dto.getName()).ne(Dept::getId, dept.getId())) > 0) {
            throw new BusinessException(400, "部门名称已存在");
        }
        if (dto.getCode() != null && !dto.getCode().equals(dept.getCode())
                && this.count(new LambdaQueryWrapper<Dept>()
                    .eq(Dept::getCode, dto.getCode()).ne(Dept::getId, dept.getId())) > 0) {
            throw new BusinessException(400, "部门编码已存在");
        }

        if (dto.getName() != null) dept.setName(dto.getName());
        if (dto.getCode() != null) dept.setCode(dto.getCode());
        if (dto.getParentId() != null) dept.setParentId(dto.getParentId());
        if (dto.getStatus() != null) dept.setStatus(dto.getStatus());

        this.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDept(Long id) {
        Dept dept = this.getById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        if (this.count(new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, id)) > 0) {
            throw new BusinessException(400, "部门下有子部门，无法删除");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, id)) > 0) {
            throw new BusinessException(400, "部门下有用户，无法删除");
        }
        this.removeById(id);
        return true;
    }

    @Override
    public List<DeptTreeVO> getDeptTree() {
        // 按 sort 升序查出全部部门，保证同级部门顺序确定（sort 越小越靠前）
        List<Dept> all = this.list(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSort));

        // 用 LinkedHashMap 保持插入顺序，兄弟节点按 sort 升序排列
        Map<String, DeptTreeVO> voById = all.stream()
                .map(this::toTreeVO)
                .collect(Collectors.toMap(DeptTreeVO::getId, vo -> vo, (a, b) -> a, LinkedHashMap::new));

        List<DeptTreeVO> tree = new ArrayList<>();
        for (DeptTreeVO node : voById.values()) {
            String parentId = node.getParentId();
            if (parentId == null) {
                tree.add(node);
            } else {
                DeptTreeVO parent = voById.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                } else {
                    // 父部门不存在（孤儿节点），兜底挂到顶层
                    tree.add(node);
                }
            }
        }

        return tree;
    }

}

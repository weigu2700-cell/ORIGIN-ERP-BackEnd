package org.smart.erp.system.service.Impl;

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
import org.smart.erp.system.vo.DeptVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    private final UserMapper userMapper;

    public DeptServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
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
    public DeptVO createDept(DeptDTO dto) {
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
        return toVOWithParent(dept);
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
    public Page<DeptVO> getDeptList(DeptListDTO dto) {
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
    public DeptVO updateDept(DeptUpdateDTO dto) {
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
        return toVOWithParent(dept);
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

}

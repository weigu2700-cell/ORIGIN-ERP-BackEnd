package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import org.smart.erp.system.dto.PermissionAddDto;
import org.smart.erp.system.dto.PermissionDetailDto;
import org.smart.erp.system.dto.PermissionUpdateDto;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionTreeVo;
import org.smart.erp.system.vo.PermissionVo;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    Page<PermissionTreeVo> pagePermission(PermissionDetailDto dto);

    List<PermissionTreeVo> getPermissionTree();

    PermissionTreeVo detailPermission(Long id);

    /** 新增权限，仅落库不返回数据 */
    void addPermission(@Valid PermissionAddDto dto);

    void updatePermission(PermissionUpdateDto dto);

    List<PermissionVo> getCurrentUserPermission();

    List<PermissionVo> detailCurrentUserPermission(Long id);
}

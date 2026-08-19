package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import org.smart.erp.system.dto.PermissionCreateDTO;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.dto.PermissionUpdateDTO;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionTreeVO;
import org.smart.erp.system.vo.PermissionVO;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    Page<PermissionTreeVO> listPermission(PermissionGetDTO dto);

    List<PermissionTreeVO> getPermissionTree();

    PermissionTreeVO getPermissionDetail(Long id);

    /** 新增权限，仅落库不返回数据 */
    void createPermission(@Valid PermissionCreateDTO dto);

    void updatePermission(PermissionUpdateDTO dto);

    List<PermissionVO> getCurrentUserPermission();

    List<PermissionVO> getCurrentUserPermissionById(Long id);
}

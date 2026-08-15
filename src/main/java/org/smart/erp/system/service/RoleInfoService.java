package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.RoleGetDTO;
import org.smart.erp.system.dto.RolePermissionAssignDTO;
import org.smart.erp.system.dto.RoleUpdateDTO;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.vo.RoleInfoVO;

public interface RoleInfoService extends IService<RoleInfo> {
    Page<RoleInfoVO> getRoleList(RoleGetDTO dto);

    RoleInfoVO updateRole(RoleUpdateDTO dto);

    RoleInfoVO addRole(RoleUpdateDTO dto);

    RoleInfoVO getRoleDetail(Long id);

    void assignPermissions(RolePermissionAssignDTO dto);
}

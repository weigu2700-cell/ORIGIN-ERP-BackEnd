package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.RoleGetDTO;
import org.smart.erp.system.dto.RoleMenuAssignDTO;
import org.smart.erp.system.dto.RolePermissionAssignDTO;
import org.smart.erp.system.dto.RoleUpdateDTO;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.vo.RoleInfoVO;

public interface RoleInfoService extends IService<RoleInfo> {
    Page<RoleInfoVO> listRole(RoleGetDTO dto);

    /** 更新角色，仅落库不返回数据 */
    void updateRole(RoleUpdateDTO dto);

    /** 新增角色，仅落库不返回数据 */
    void createRole(RoleUpdateDTO dto);

    RoleInfoVO getRoleDetail(Long id);

    void assignPermissions(RolePermissionAssignDTO dto);


    void removeRole(Long id);

    void assignUsers(RoleMenuAssignDTO dto);
}

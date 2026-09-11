package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.RoleDetailDto;
import org.smart.erp.system.dto.RoleMenuAssignDto;
import org.smart.erp.system.dto.RolePermissionAssignDto;
import org.smart.erp.system.dto.RoleUpdateDto;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.vo.RoleInfoVo;

public interface RoleInfoService extends IService<RoleInfo> {
    Page<RoleInfoVo> pageRole(RoleDetailDto dto);

    /** 更新角色，仅落库不返回数据 */
    void updateRole(RoleUpdateDto dto);

    /** 新增角色，仅落库不返回数据 */
    void addRole(RoleUpdateDto dto);

    RoleInfoVo detailRole(Long id);

    void assignPermissions(RolePermissionAssignDto dto);


    void removeRole(Long id);

    void assignUsers(RoleMenuAssignDto dto);
}

package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.vo.PermissionVO;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    Page<PermissionVO> getPermissionList(PermissionGetDTO dto);

    List<PermissionVO> getPermissionTree();
}

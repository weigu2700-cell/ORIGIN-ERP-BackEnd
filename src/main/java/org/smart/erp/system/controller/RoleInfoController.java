package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.RoleGetDTO;
import org.smart.erp.system.dto.RolePermissionAssignDTO;
import org.smart.erp.system.dto.RoleUpdateDTO;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.service.RoleInfoService;
import org.smart.erp.system.vo.RoleInfoVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/role")
public class RoleInfoController {

    private final RoleInfoService roleInfoService;

    public RoleInfoController(RoleInfoService roleInfoService) {
        this.roleInfoService = roleInfoService;
    }


    @GetMapping("/list")
    public Result<Page<RoleInfoVO>> getRoleList(RoleGetDTO dto) {
        return Result.success(roleInfoService.getRoleList(dto));
    }

    @GetMapping("/{id}")
    public Result<RoleInfoVO> getRoleDetail(@PathVariable Long id) {
        return Result.success(roleInfoService.getRoleDetail(id));
    }

    @PutMapping("/{id}")
    public Result<RoleInfoVO> updateRole(@RequestBody RoleUpdateDTO dto) {
        return Result.success(roleInfoService.updateRole(dto));
    }

    @PostMapping("/add")
    public Result<RoleInfoVO> addRole(@RequestBody RoleUpdateDTO dto) {
        return Result.success(roleInfoService.addRole(dto));
    }

    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @RequestBody RolePermissionAssignDTO dto) {
        dto.setRoleId(id);
        roleInfoService.assignPermissions(dto);
        return Result.success(null);
    }
}

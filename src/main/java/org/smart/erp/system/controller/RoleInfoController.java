package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.RoleGetDTO;
import org.smart.erp.system.dto.RoleMenuAssignDTO;
import org.smart.erp.system.dto.RolePermissionAssignDTO;
import org.smart.erp.system.dto.RoleUpdateDTO;
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
    public Result<Void> updateRole(@RequestBody RoleUpdateDTO dto) {
        roleInfoService.updateRole(dto);
        return Result.success();
    }

    @PostMapping("/add")
    public Result<Void> addRole(@RequestBody RoleUpdateDTO dto) {
        roleInfoService.addRole(dto);
        return Result.success();
    }

    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @RequestBody RolePermissionAssignDTO dto) {
        dto.setRoleId(id);
        roleInfoService.assignPermissions(dto);
        return Result.success(null);
    }

    @PostMapping("/{id}/menu")
    public Result<Void> assignUsers(@PathVariable Long id,
                                     @RequestBody RoleMenuAssignDTO dto) {
        dto.setRoleId(id);
        roleInfoService.assignUsers(dto);
        return Result.success(null);
    }


    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleInfoService.removeRole(id);
        return Result.success(null);
    }


}

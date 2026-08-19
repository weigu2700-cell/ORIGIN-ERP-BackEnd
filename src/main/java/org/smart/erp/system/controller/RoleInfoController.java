package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "角色管理", description = "角色的增删改查、分配权限与菜单")
public class RoleInfoController {

    private final RoleInfoService roleInfoService;

    public RoleInfoController(RoleInfoService roleInfoService) {
        this.roleInfoService = roleInfoService;
    }


    @Operation(summary = "角色分页列表")
    @GetMapping("/list")
    public Result<Page<RoleInfoVO>> listRole(RoleGetDTO dto) {
        return Result.success(roleInfoService.listRole(dto));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public Result<RoleInfoVO> getRoleDetail(@PathVariable Long id) {
        return Result.success(roleInfoService.getRoleDetail(id));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Void> updateRole(@RequestBody RoleUpdateDTO dto) {
        roleInfoService.updateRole(dto);
        return Result.success();
    }

    @Operation(summary = "新增角色")
    @PostMapping("/add")
    public Result<Void> createRole(@RequestBody RoleUpdateDTO dto) {
        roleInfoService.createRole(dto);
        return Result.success();
    }

    @Operation(summary = "为角色分配权限")
    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @RequestBody RolePermissionAssignDTO dto) {
        dto.setRoleId(id);
        roleInfoService.assignPermissions(dto);
        return Result.success(null);
    }

    @Operation(summary = "为角色分配菜单")
    @PostMapping("/{id}/menu")
    public Result<Void> assignUsers(@PathVariable Long id,
                                     @RequestBody RoleMenuAssignDTO dto) {
        dto.setRoleId(id);
        roleInfoService.assignUsers(dto);
        return Result.success(null);
    }


    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleInfoService.removeRole(id);
        return Result.success(null);
    }


}

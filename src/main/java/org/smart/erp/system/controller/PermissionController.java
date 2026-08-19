package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.PermissionCreateDTO;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.dto.PermissionUpdateDTO;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionTreeVO;
import org.smart.erp.system.vo.PermissionVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/permission")
@Tag(name = "权限管理", description = "权限的增删查、列表与树形结构")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "权限分页列表")
    @GetMapping("/list")
    public Result<Page<PermissionTreeVO>> listPermission(@Valid PermissionGetDTO dto) {
        return Result.success(permissionService.listPermission(dto));
    }

    @Operation(summary = "权限树形结构")
    @GetMapping("/tree")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @Operation(summary = "权限详情")
    @GetMapping("/{id}")
    public Result<PermissionTreeVO> getPermissionDetail(@PathVariable Long id) {
        return Result.success(permissionService.getPermissionDetail(id));
    }

    @Operation(summary = "新增权限")
    @PostMapping("/add")
    public Result<Void> createPermission(@RequestBody @Valid PermissionCreateDTO dto) {
        permissionService.createPermission(dto);
        return Result.success();
    }

    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    public Result<Void> updatePermission(PermissionUpdateDTO dto) {
        permissionService.updatePermission(dto);
        return Result.success();
    }

    @Operation(summary = "当前用户权限列表")
    @GetMapping
    public Result<List<PermissionVO>> getCurrentUserPermission() {
        return Result.success(permissionService.getCurrentUserPermission());
    }
}

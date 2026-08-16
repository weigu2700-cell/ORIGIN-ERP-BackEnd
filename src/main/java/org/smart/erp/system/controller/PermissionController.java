package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.PermissionCreateDTO;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionTreeVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/list")
    public Result<Page<PermissionTreeVO>> getPermissionList(@Valid PermissionGetDTO dto) {
        return Result.success(permissionService.getPermissionList(dto));
    }

    @GetMapping("/tree")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @GetMapping("/{id}")
    public Result<PermissionTreeVO> getPermissionDetail(@PathVariable Long id) {
        return Result.success(permissionService.getPermissionDetail(id));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Valid PermissionCreateDTO dto) {
        permissionService.createPermission(dto);
        return Result.success();
    }
}

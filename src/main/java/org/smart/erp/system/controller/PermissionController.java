package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.PermissionGetDTO;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.vo.PermissionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/list")
    public Result<IPage<PermissionVO>> getPermissionList(@Valid PermissionGetDTO dto) {
        return Result.success(permissionService.getPermissionList(dto));
    }

    @GetMapping("/tree")
    public Result<List<PermissionVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }
}

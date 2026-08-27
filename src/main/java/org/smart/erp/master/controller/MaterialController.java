package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.MaterialDTO.MaterialCreateDTO;
import org.smart.erp.master.dto.MaterialDTO.MaterialListDTO;
import org.smart.erp.master.dto.MaterialDTO.MaterialUpdateDTO;
import org.smart.erp.master.enums.MaterialStatus;
import org.smart.erp.master.service.MaterialService;
import org.smart.erp.master.vo.MaterialVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/master/material")
@Tag(name = "物料管理", description = "物料的增删改查、详情与状态变更")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @Operation(summary = "新增物料")
    @PreAuthorize("hasAnyAuthority('master:material:create')")
    @PostMapping
    public Result<Void> create(@RequestBody MaterialCreateDTO dto) {
        materialService.createMaterial(dto);
        return Result.success();
    }

    @Operation(summary = "物料分页列表")
    @PreAuthorize("hasAnyAuthority('master:material:list')")
    @GetMapping
    public Result<Page<MaterialVO>> list(MaterialListDTO dto) {
        return Result.success(materialService.listMaterial(dto));
    }

    @Operation(summary = "物料详情")
    @PreAuthorize("hasAnyAuthority('master:material:get')")
    @GetMapping("/{id}")
    public Result<MaterialVO> getMaterialDetail(@PathVariable Long id) {
        return Result.success(materialService.getMaterialDetail(id));
    }

    @Operation(summary = "更新物料")
    @PreAuthorize("hasAnyAuthority('master:material:update')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MaterialUpdateDTO dto) {
        materialService.updateMaterial(id, dto);
        return Result.success();
    }

    @Operation(summary = "变更物料状态")
    @PreAuthorize("hasAnyAuthority('master:material:status')")
    @PutMapping("/{id}/status")
    public Result<Void> changeMaterialStatus(@PathVariable Long id, @RequestBody MaterialStatus status) {
        materialService.changeMaterialStatus(id, status);
        return Result.success();
    }
}

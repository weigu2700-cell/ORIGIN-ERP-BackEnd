package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierAddDto;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierPageDto;
import org.smart.erp.master.dto.MaterialSupplierDto.MaterialSupplierUpdateDto;
import org.smart.erp.master.service.MaterialSupplierService;
import org.smart.erp.master.vo.MaterialSupplierVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("master/material-supplier")
@Tag(name = "物料供应商管理", description = "物料与供应商关联关系的增删改查、状态与优选变更")
public class MaterialSupplierController {

    private final MaterialSupplierService materialSupplierService;

    public MaterialSupplierController(MaterialSupplierService materialSupplierService) {
        this.materialSupplierService = materialSupplierService;
    }

    @Operation(summary = "新增物料供应商关联")
    @PreAuthorize("hasAnyAuthority('master:material-supplier:create')")
    @PostMapping
    public Result<Void> add(@RequestBody MaterialSupplierAddDto dto) {
        materialSupplierService.addMaterialSupplier(dto);
        return Result.success();
    }

    @Operation(summary = "物料供应商关联分页列表")
    @GetMapping
    public Result<Page<MaterialSupplierVo>> list(MaterialSupplierPageDto dto) {
        return Result.success(materialSupplierService.pageMaterialSupplier(dto));
    }

    @Operation(summary = "物料供应商关联详情")
    @GetMapping("/{id}")
    public Result<MaterialSupplierVo> detailMaterialSupplier(@PathVariable Long id) {
        return Result.success(materialSupplierService.getMaterialSupplier(id));
    }

    @Operation(summary = "更新物料供应商关联")
    @PreAuthorize("hasAnyAuthority('master:material-supplier:update')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MaterialSupplierUpdateDto dto) {
        materialSupplierService.updateMaterialSupplier(id, dto);
        return Result.success();
    }

    @Operation(summary = "变更物料供应商关联状态")
    @PreAuthorize("hasAnyAuthority('master:material-supplier:status')")
    @PutMapping("/{id}/status")
    public Result<Void> changeMaterialSupplierStatus(@PathVariable Long id) {
        materialSupplierService.changeMaterialSupplierStatus(id);
        return Result.success();
    }

    @Operation(summary = "设置优选供应商")
    @PutMapping("/{id}/preferred")
    public Result<Void> preferred(@PathVariable Long masterId, Long supplier) {
        materialSupplierService.changeMaterialSupplierPreferred(masterId, supplier);
        return Result.success();
    }

    @Operation(summary = "物料供应商关联导出")
    @GetMapping("/export")
    public Result<Void> export(List<Long> ids, HttpServletResponse response) {
        materialSupplierService.exportMaterialSupplier(ids, response);
        return Result.success();
    }

}

package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierCreateDTO;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierListDTO;
import org.smart.erp.master.dto.MaterialSupplierDTO.MaterialSupplierUpdateDTO;
import org.smart.erp.master.service.MaterialSupplierService;
import org.smart.erp.master.vo.MaterialSupplierVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("master/material-supplier")
public class MaterialSupplierController {

    private final MaterialSupplierService materialSupplierService;

    public MaterialSupplierController(MaterialSupplierService materialSupplierService) {
        this.materialSupplierService = materialSupplierService;
    }


    @PreAuthorize("hasAnyAuthority('master:material-supplier:create')")
    @PostMapping
    public Result<Void> create(@RequestBody MaterialSupplierCreateDTO dto) {
        materialSupplierService.createMaterialSupplier(dto);
        return Result.success();
    }

    @GetMapping
    public Result<Page<MaterialSupplierVO>> list(MaterialSupplierListDTO dto) {
        return Result.success(materialSupplierService.listMaterialSupplier(dto));
    }

    @GetMapping("/{id}")
    public Result<MaterialSupplierVO> get(@PathVariable Long id) {
        return Result.success(materialSupplierService.getMaterialSupplier(id));
    }

    @PreAuthorize("hasAnyAuthority('master:material-supplier:update')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MaterialSupplierUpdateDTO dto) {
        materialSupplierService.updateMaterialSupplier(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('master:material-supplier:status')")
    @PutMapping("/{id}/status")
    public Result<Void> change(@PathVariable Long id) {
        materialSupplierService.changeMaterialSupplierStatus(id);
        return Result.success();
    }

    @PutMapping("/{id}/preferred")
    public Result<Void> preferred(@PathVariable Long masterId , Long supplier) {
        materialSupplierService.changeMaterialSupplierPreferred(masterId, supplier);
        return Result.success();
    }

}

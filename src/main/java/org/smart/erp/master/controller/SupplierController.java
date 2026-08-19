package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.SupplierDTO.SupplierCreateDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierListDTO;
import org.smart.erp.master.dto.SupplierDTO.SupplierUpdateDTO;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.service.SupplierService;
import org.smart.erp.master.vo.SupplierVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/master/supplier")
@RequiredArgsConstructor
@Tag(name = "供应商管理", description = "供应商的增删改查")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "新增供应商")
    @PostMapping
    @PreAuthorize("hasAuthority('master:supplier:create')")
    public Result<Void> create(@Valid @RequestBody SupplierCreateDTO dto) {
        supplierService.createSupplier(dto);
        return Result.success();
    }

    @Operation(summary = "供应商分页列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('master:supplier:list')")
    public Result<Page<SupplierVO>> listSupplier(SupplierListDTO dto) {
        return Result.success(supplierService.listSupplier(dto));
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:supplier:get')")
    public Result<SupplierVO> getSupplierDetail(@PathVariable Long id) {
        return Result.success(supplierService.getSupplierDetail(id));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:supplier:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody SupplierUpdateDTO dto) {
        supplierService.updateSupplier(id, dto);
        return Result.success();
    }

    @Operation(summary = "更改供应商状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:supplier:status')")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody SupplierStatus status) {
        supplierService.changeSupplierStatus(id, status);
        return Result.success();
    }
}

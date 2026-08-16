package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.apache.ibatis.annotations.Update;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.SupplierCreateDTO;
import org.smart.erp.master.dto.SupplierListDTO;
import org.smart.erp.master.dto.SupplierUpdateDTO;
import org.smart.erp.master.entity.Supplier;
import org.smart.erp.master.enums.SupplierStatus;
import org.smart.erp.master.service.SupplierService;
import org.smart.erp.master.vo.SupplierVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supplier")
@AllArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public Result<Void> create(@Validated @RequestBody SupplierCreateDTO dto) {
        supplierService.createSupplier(dto);
        return Result.success();
    }

    @GetMapping
    public Result<Page<SupplierVO>> list(@PathVariable SupplierListDTO dto) {
        return Result.success(supplierService.listSupplier(dto));
    }

    @GetMapping("/{id}")
    public Result<SupplierVO> getDetail(@PathVariable Long id) {
        return Result.success(supplierService.getSupplierDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody SupplierUpdateDTO dto) {
        supplierService.updateSupplier(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @Validated SupplierStatus status) {
        supplierService.changeSupplierStatus(id, status);
        return Result.success();
    }
}

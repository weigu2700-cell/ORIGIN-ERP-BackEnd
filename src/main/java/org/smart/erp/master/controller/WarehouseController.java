package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseCreateDTO;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseListDTO;
import org.smart.erp.master.dto.WarehouseDTO.WarehouseUpdateDTO;
import org.smart.erp.master.enums.WarehouseStatus;
import org.smart.erp.master.service.WarehouseService;
import org.smart.erp.master.vo.WarehouseVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("master/warehouse")
@Tag(name = "仓库管理", description = "仓库的增删改查")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PreAuthorize("hasAnyAuthority('master:warehouse:create')")
    @PostMapping
    public Result<Void> create(@RequestBody @Validated WarehouseCreateDTO dto) {
        warehouseService.create(dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('master:warehouse:list')")
    @GetMapping
    public Result<Page<WarehouseVO>> list(WarehouseListDTO dto) {
        return Result.success(warehouseService.getWarehouseList(dto));
    }

    @PreAuthorize("hasAnyAuthority('master:warehouse:get')")
    @GetMapping("/{id}")
    public Result<WarehouseVO> getWarehouseDetail(@PathVariable Long id) {
        return Result.success(warehouseService.getWarehouse(id));
    }

    @PreAuthorize("hasAnyAuthority('master:warehouse:update')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated WarehouseUpdateDTO dto) {
        warehouseService.updateWarehouse(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('master:warehouse:status')")
    @PutMapping("/{id}/status")
    public Result<Void> changeWarehouseStatus(@PathVariable Long id, WarehouseStatus status) {
        warehouseService.updateWarehouseStatus(id, status);
        return Result.success();
    }
}

package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.WarehouseDto.WarehouseAddDto;
import org.smart.erp.master.dto.WarehouseDto.WarehousePageDto;
import org.smart.erp.master.dto.WarehouseDto.WarehouseStatusChangeDto;
import org.smart.erp.master.dto.WarehouseDto.WarehouseUpdateDto;
import org.smart.erp.master.service.WarehouseService;
import org.smart.erp.master.vo.WarehouseVo;
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

    @Operation(summary = "新增仓库")
    @PreAuthorize("hasAnyAuthority('master:warehouse:create')")
    @PostMapping
    public Result<Void> add(@RequestBody @Validated WarehouseAddDto dto) {
        warehouseService.add(dto);
        return Result.success();
    }

    @Operation(summary = "仓库分页列表")
    @PreAuthorize("hasAnyAuthority('master:warehouse:list')")
    @GetMapping
    public Result<Page<WarehouseVo>> list(WarehousePageDto dto) {
        return Result.success(warehouseService.getWarehouseList(dto));
    }

    @Operation(summary = "仓库详情")
    @PreAuthorize("hasAnyAuthority('master:warehouse:get')")
    @GetMapping("/{id}")
    public Result<WarehouseVo> detailWarehouse(@PathVariable Long id) {
        return Result.success(warehouseService.getWarehouse(id));
    }

    @Operation(summary = "更新仓库")
    @PreAuthorize("hasAnyAuthority('master:warehouse:update')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated WarehouseUpdateDto dto) {
        warehouseService.updateWarehouse(id, dto);
        return Result.success();
    }

    @Operation(summary = "变更仓库状态")
    @PreAuthorize("hasAnyAuthority('master:warehouse:status')")
    @PutMapping("/{id}/status")
    public Result<Void> changeWarehouseStatus(@PathVariable Long id, @RequestBody WarehouseStatusChangeDto dto) {
        warehouseService.updateWarehouseStatus(id, dto.getStatus());
        return Result.success();
    }
}

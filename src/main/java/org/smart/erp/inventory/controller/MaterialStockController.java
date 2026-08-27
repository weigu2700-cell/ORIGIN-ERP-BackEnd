package org.smart.erp.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.inventory.dto.materialStockDto.CreateDto;
import org.smart.erp.inventory.dto.materialStockDto.ListDto;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.vo.MaterialStockVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "物料库存", description = "库存的创建、分页列表与详情查询")
@RestController
@RequestMapping("inventory/material-stock")
public class MaterialStockController {

    private final MaterialStockService materialStockService;

    public MaterialStockController(MaterialStockService materialStockService) {
        this.materialStockService = materialStockService;
    }

    @Operation(summary = "新增库存记录")
    @PostMapping
    public Result<MaterialStockVO> create(@RequestBody @Validated CreateDto dto) {
        return Result.success(materialStockService.createMaterialStock(dto));
    }

    @Operation(summary = "库存分页列表")
    @GetMapping
    public Result<Page<MaterialStockVO>> list(ListDto dto) {
        return Result.success(materialStockService.listMaterialStock(dto));
    }

    @Operation(summary = "库存详情")
    @GetMapping("/{id}")
    public Result<MaterialStockVO> get(@PathVariable Long id) {
        return Result.success(materialStockService.getMaterialStock(id));
    }
}

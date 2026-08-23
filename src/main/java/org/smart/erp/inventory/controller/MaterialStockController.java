package org.smart.erp.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.inventory.dto.materialStockDto.CreateDto;
import org.smart.erp.inventory.dto.materialStockDto.ListDto;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.vo.MaterialStockVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("inventory/material-stock")
public class MaterialStockController {

    private final MaterialStockService materialStockService;

    public MaterialStockController(MaterialStockService materialStockService) {
        this.materialStockService = materialStockService;
    }

    @PostMapping
    public Result<MaterialStockVO> create(@RequestBody @Validated CreateDto dto) {
        return Result.success(materialStockService.createMaterialStock(dto));
    }

    @GetMapping
    public Result<Page<MaterialStockVO>> list(@RequestParam ListDto dto) {
        return Result.success(materialStockService.listMaterialStock(dto));
    }

    @GetMapping("/{id}")
    public Result<MaterialStockVO> get(@PathVariable Long id) {
        return Result.success(materialStockService.getMaterialStock(id));
    }
}

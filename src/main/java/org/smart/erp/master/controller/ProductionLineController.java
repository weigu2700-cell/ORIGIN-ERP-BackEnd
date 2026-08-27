package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineCreateDTO;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineListDTO;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineUpdateDTO;
import org.smart.erp.master.entity.ProductionLine;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.smart.erp.master.service.ProductionLineService;
import org.smart.erp.master.vo.ProductionLineVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/master/production_line")
@Tag(name = "生产线管理", description = "生产线的增删改查、详情与状态变更")
public class ProductionLineController {

    private final ProductionLineService productionLineService;

    public ProductionLineController(ProductionLineService productionLineService) {
        this.productionLineService = productionLineService;
    }

    @Operation(summary = "新增生产线")
    @PreAuthorize("hasAnyAuthority('master:production_line:create')")
    @PostMapping
    public Result<Void> createProductionLine(@RequestBody ProductionLineCreateDTO dto) {
        productionLineService.createProductionLine(dto);
        return Result.success();
    }

    @Operation(summary = "生产线分页列表")
    @PreAuthorize("hasAnyAuthority('master:production_line:list')")
    @GetMapping
    public Result<Page<ProductionLineVO>> listProductionLine(ProductionLineListDTO dto) {
        return Result.success(productionLineService.listProductionLine(dto));
    }

    @Operation(summary = "生产线详情")
    @PreAuthorize("hasAnyAuthority('master:production_line:get')")
    @GetMapping("/{id}")
    public Result<ProductionLineVO> getProductionLineDetail(@PathVariable Long id) {
        return Result.success(productionLineService.getProductionLine(id));
    }

    @Operation(summary = "更新生产线")
    @PreAuthorize("hasAnyAuthority('master:production_line:update')")
    @PutMapping("/{id}")
    public Result<Void> updateProductionLine(@PathVariable Long id, @RequestBody ProductionLineUpdateDTO dto) {
        productionLineService.updateProductionLine(id, dto);
        return Result.success();
    }

    @Operation(summary = "变更生产线状态")
    @PreAuthorize("hasAnyAuthority('master:production_line:status')")
    @PutMapping("/{id}/status")
    public Result<Void> updateProductionLineStatus(@PathVariable Long id, ProductionLineStatus status) {
        productionLineService.updateProductionLineStatus(id, status);
        return Result.success();
    }
}

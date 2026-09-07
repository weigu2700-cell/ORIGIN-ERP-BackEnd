package org.smart.erp.production.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.production.dto.createProductionOrderDto;
import org.smart.erp.production.dto.pageProductionOrderDto;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.production.vo.MaterialRequirementVo;
import org.smart.erp.production.vo.ProductionOrderVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"prd/order", "/production/order"})
@Tag(name="生产订单管理", description="生产订单管理")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('production:order:create')")
    @Operation(summary = "创建生产订单")
    public Result<Void> create(
            @RequestBody @Parameter(description = "创建生产订单参数") createProductionOrderDto dto) {
        productionOrderService.createProductionOrder(dto);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('production:order:list')")
    @Operation(summary = "分页获取生产订单")
    public Result<Page<ProductionOrderVo>> page(
            @ModelAttribute @Parameter(description = "分页获取生产订单") pageProductionOrderDto dto) {
        return Result.success(productionOrderService.pageProductionOrder(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('production:order:get')")
    @Operation(summary = "获取生产订单详情")
    public Result<ProductionOrderVo> getById(@PathVariable @Parameter(description = "生产订单ID") Long id) {
        return Result.success(productionOrderService.DetailProductionOrder(id));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyAuthority('production:order:start')")
    @Operation(summary = "开始生产订单")
    public Result<Void> start(@PathVariable @Parameter(description = "生产订单ID") Long id) {
        productionOrderService.startProductionOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('production:order:complete')")
    @Operation(summary = "完成生产订单")
    public Result<Void> complete(@PathVariable @Parameter(description = "生产订单ID") Long id) {
        productionOrderService.completeProductionOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('production:order:cancel')")
    @Operation(summary = "取消生产订单")
    public Result<Void> cancel(@PathVariable @Parameter(description = "生产订单ID") Long id) {
        productionOrderService.cancelProductionOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/release")
    @PreAuthorize("hasAnyAuthority('production:order:release')")
    @Operation(summary = "下达生产订单（按 BOM 净需求自动生成采购需求与草稿采购订单，并返回物料需求结果）")
    public Result<List<MaterialRequirementVo>> release(
            @PathVariable @Parameter(description = "生产订单ID") Long id) {
        return Result.success(productionOrderService.releaseProductionOrder(id));
    }

}

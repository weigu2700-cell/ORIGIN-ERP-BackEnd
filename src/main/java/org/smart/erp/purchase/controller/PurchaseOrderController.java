package org.smart.erp.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.purchase.dto.CreatePurchaseOrderDto;
import org.smart.erp.purchase.dto.PagePurchaseOrderDto;
import org.smart.erp.purchase.dto.UpdatePurchaseOrderDto;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.purchase.vo.PurchaseOrderVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase/order")
@Tag(name = "采购订单管理", description = "采购订单管理")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('purchase:order:create')")
    @Operation(summary = "创建采购订单")
    public Result<Void> create(
            @RequestBody @Parameter(description = "创建采购订单参数") CreatePurchaseOrderDto dto) {
        purchaseOrderService.createPurchaseOrder(dto);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('purchase:order:list')")
    @Operation(summary = "分页查询采购订单")
    public Result<Page<PurchaseOrderVo>> page(
            @Parameter(description = "分页查询参数") PagePurchaseOrderDto dto) {
        return Result.success(purchaseOrderService.pagePurchaseOrder(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('purchase:order:get')")
    @Operation(summary = "采购订单详情")
    public Result<PurchaseOrderVo> detail(@PathVariable @Parameter(description = "采购订单ID") Long id) {
        return Result.success(purchaseOrderService.detailPurchaseOrder(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('purchase:order:update')")
    @Operation(summary = "编辑草稿采购订单（补全供应商/单价/交期）")
    public Result<Void> update(@PathVariable @Parameter(description = "采购订单ID") Long id,
                               @RequestBody UpdatePurchaseOrderDto dto) {
        purchaseOrderService.updatePurchaseOrder(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('purchase:order:approve')")
    @Operation(summary = "审批采购订单")
    public Result<Void> approve(@PathVariable @Parameter(description = "采购订单ID") Long id) {
        purchaseOrderService.approvePurchaseOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasAnyAuthority('purchase:order:ship')")
    @Operation(summary = "采购订单发货")
    public Result<Void> ship(@PathVariable @Parameter(description = "采购订单ID") Long id) {
        purchaseOrderService.shipPurchaseOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyAuthority('purchase:order:receive')")
    @Operation(summary = "采购订单收货")
    public Result<Void> receive(@PathVariable @Parameter(description = "采购订单ID") Long id) {
        purchaseOrderService.receivePurchaseOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('purchase:order:close')")
    @Operation(summary = "关闭采购订单")
    public Result<Void> close(@PathVariable @Parameter(description = "采购订单ID") Long id) {
        purchaseOrderService.closePurchaseOrder(id);
        return Result.success();
    }
}

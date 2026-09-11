package org.smart.erp.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.purchase.dto.PurchaseDemandAddDto;
import org.smart.erp.purchase.dto.PurchaseDemandPageDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.service.PurchaseDemandService;
import org.smart.erp.purchase.vo.PurchaseDemandVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase/demand")
@Tag(name = "采购需求管理", description = "采购需求管理")
public class PurchaseDemandController {

    private final PurchaseDemandService purchaseDemandService;

    public PurchaseDemandController(PurchaseDemandService purchaseDemandService) {
        this.purchaseDemandService = purchaseDemandService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('purchase:demand:create')")
    @Operation(summary = "创建采购需求")
    public Result<PurchaseDemand> add(
            @RequestBody @Parameter(description = "创建采购需求参数") PurchaseDemandAddDto dto) {
        return Result.success(purchaseDemandService.addPurchaseDemand(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('purchase:demand:list')")
    @Operation(summary = "分页查询采购需求")
    public Result<Page<PurchaseDemandVo>> page(
            @Parameter(description = "分页查询参数") PurchaseDemandPageDto dto) {
        return Result.success(purchaseDemandService.pagePurchaseDemand(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('purchase:demand:get')")
    @Operation(summary = "采购需求详情")
    public Result<PurchaseDemandVo> detail(@PathVariable @Parameter(description = "采购需求ID") Long id) {
        return Result.success(purchaseDemandService.detailPurchaseDemand(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('purchase:demand:approve')")
    @Operation(summary = "审批采购需求")
    public Result<Void> approve(@PathVariable @Parameter(description = "采购需求ID") Long id) {
        purchaseDemandService.approvePurchaseDemand(id);
        return Result.success();
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('purchase:demand:close')")
    @Operation(summary = "关闭采购需求")
    public Result<Void> close(@PathVariable @Parameter(description = "采购需求ID") Long id) {
        purchaseDemandService.closePurchaseDemand(id);
        return Result.success();
    }
}

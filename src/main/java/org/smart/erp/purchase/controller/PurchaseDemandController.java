package org.smart.erp.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.purchase.dto.CreatePurchaseDemandDto;
import org.smart.erp.purchase.dto.PagePurchaseDemandDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.service.PurchaseDemandService;
import org.smart.erp.purchase.vo.PurchaseDemandVo;
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
    @Operation(summary = "创建采购需求")
    public Result<PurchaseDemand> create(
            @RequestBody @Parameter(description = "创建采购需求参数") CreatePurchaseDemandDto dto) {
        return Result.success(purchaseDemandService.createPurchaseDemand(dto));
    }

    @GetMapping
    @Operation(summary = "分页查询采购需求")
    public Result<Page<PurchaseDemandVo>> page(
            @Parameter(description = "分页查询参数") PagePurchaseDemandDto dto) {
        return Result.success(purchaseDemandService.pagePurchaseDemand(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "采购需求详情")
    public Result<PurchaseDemandVo> detail(@PathVariable @Parameter(description = "采购需求ID") Long id) {
        return Result.success(purchaseDemandService.detailPurchaseDemand(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "审批采购需求")
    public Result<Void> approve(@PathVariable @Parameter(description = "采购需求ID") Long id) {
        purchaseDemandService.approvePurchaseDemand(id);
        return Result.success();
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭采购需求")
    public Result<Void> close(@PathVariable @Parameter(description = "采购需求ID") Long id) {
        purchaseDemandService.closePurchaseDemand(id);
        return Result.success();
    }
}

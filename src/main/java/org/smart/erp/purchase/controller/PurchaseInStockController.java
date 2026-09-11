package org.smart.erp.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.smart.erp.common.result.Result;
import org.smart.erp.purchase.dto.PagePurchaseInStockDto;
import org.smart.erp.purchase.dto.UploadPurchaseInStockDto;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.smart.erp.purchase.vo.PurchaseInStockVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase/in/stock")
public class PurchaseInStockController {

    private final PurchaseInStockService purchaseInStockService;

    public PurchaseInStockController(PurchaseInStockService purchaseInStockService) {
        this.purchaseInStockService = purchaseInStockService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('purchase:in:stock:list')")
    @Operation(summary = "分页查询入库单")
    public Result<Page<PurchaseInStockVo>> page(
            @Parameter(description = "分页查询参数") PagePurchaseInStockDto queryDto) {
        return Result.success(purchaseInStockService.getPagePurchaseInStock(queryDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('purchase:in:stock:detail')")
    @Operation(summary = "查询入库单详情")
    public Result<PurchaseInStockVo> get(@PathVariable Long id) {
        return Result.success(purchaseInStockService.getPurchaseInStock(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('purchase:in:stock:approve')")
    @Operation(summary = "审核入库单(移动端接口)")
    public Result<Void> approve(
            @Parameter(description = "入库单ID") @PathVariable  Long id) {
        purchaseInStockService.approvePurchaseInStock(id);
        return Result.success();
    }

    @PutMapping("/{id}/upload")
    @PreAuthorize("hasAnyAuthority('purchase:in:stock:upload')")
    @Operation(summary = "上架入库单(移动端接口)")
    public Result<Void> upload(
            @Parameter(description = "入库单ID") @PathVariable  Long id,
            @Parameter(description = "上架入库单参数") @RequestBody UploadPurchaseInStockDto dto) {
        purchaseInStockService.uploadPurchaseInStock(id, dto);
        return Result.success();
    }

}


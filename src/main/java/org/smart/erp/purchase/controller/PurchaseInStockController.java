package org.smart.erp.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.purchase.dto.PagePurchaseInStockDto;
import org.smart.erp.purchase.service.PurchaseInStockService;
import org.smart.erp.purchase.vo.PurchaseInStockVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchase/in/stock")
public class PurchaseInStockController {

    private final PurchaseInStockService purchaseInStockService;

    public PurchaseInStockController(PurchaseInStockService purchaseInStockService) {
        this.purchaseInStockService = purchaseInStockService;
    }

    @GetMapping
    public Result<Page<PurchaseInStockVo>> page(PagePurchaseInStockDto queryDto) {
        return Result.success(purchaseInStockService.getPagePurchaseInStock(queryDto));
    }
}


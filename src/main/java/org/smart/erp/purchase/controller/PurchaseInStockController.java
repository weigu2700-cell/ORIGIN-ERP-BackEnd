package org.smart.erp.purchase.controller;

import org.smart.erp.purchase.service.PurchaseInStockService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchase/in/stock")
public class PurchaseInStockController {

    private PurchaseInStockService purchaseInStockService;

    public PurchaseInStockController(PurchaseInStockService purchaseInStockService) {
        this.purchaseInStockService = purchaseInStockService;
    }
}

package org.smart.erp.purchase.controller;

import org.smart.erp.purchase.service.PurchaseDemandService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchase/demand")
public class PurchaseDemandController {

    private PurchaseDemandService purchaseDemandService;

    public PurchaseDemandController(PurchaseDemandService purchaseDemandService) {
        this.purchaseDemandService = purchaseDemandService;
    }
}

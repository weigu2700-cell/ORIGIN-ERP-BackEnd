package org.smart.erp.production.controller;

import org.smart.erp.production.service.BOMItemService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("prd/bom/item")
public class BOMItemController {

    private final BOMItemService bomItemService;

    public BOMItemController(BOMItemService bomItemService) {
        this.bomItemService = bomItemService;
    }


}

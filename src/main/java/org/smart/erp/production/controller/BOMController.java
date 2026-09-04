package org.smart.erp.production.controller;

import org.smart.erp.production.service.BOMService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("prd/bom")
public class BOMController {

    private final BOMService bomService;

    public BOMController(BOMService bomService) {
        this.bomService = bomService;
    }
}

package org.smart.erp.production.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.production.service.ProductionDemandService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/production/demand")
@Tag(name = "生产需求管理", description = "生产需求管理")
public class ProductionDemandController {

    private final ProductionDemandService productionDemandService;

    public ProductionDemandController(
            ProductionDemandService productionDemandService
    )
    {
        this.productionDemandService = productionDemandService;
    }


}

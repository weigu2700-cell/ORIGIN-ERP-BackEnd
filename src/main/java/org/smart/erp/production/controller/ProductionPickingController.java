package org.smart.erp.production.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.production.entity.ProductionPicking;
import org.smart.erp.production.service.ProductionPickingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prd/picking")
public class ProductionPickingController {

    private final ProductionPickingService productionPickingService;

    public ProductionPickingController(ProductionPickingService productionPickingService) {
        this.productionPickingService = productionPickingService;
    }

    @PostMapping("/confirm")
    public void confirmPicking(@RequestParam Long id) {
        productionPickingService.confirmPicking(id);
    }

    @GetMapping("/list")
    public List<ProductionPicking> listByOrder(@RequestParam Long productionOrderId) {
        return productionPickingService.list(new LambdaQueryWrapper<ProductionPicking>()
                .eq(ProductionPicking::getProductionOrderId, productionOrderId)
                .orderByAsc(ProductionPicking::getCreateTime));
    }
}

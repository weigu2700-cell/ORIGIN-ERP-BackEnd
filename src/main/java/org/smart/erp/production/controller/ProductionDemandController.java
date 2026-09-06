package org.smart.erp.production.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.production.dto.pageProductionDemandDto;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.production.vo.ProductionDemandVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(summary = "分页查询生产需求")
    public Result<Page<ProductionDemandVo>> page(
            @RequestBody @Parameter(description = "分页查询生产需求") pageProductionDemandDto dto) {
        return Result.success(productionDemandService.pageProductionDemand(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询生产需求详情")
    public Result<ProductionDemandVo> get(
            @PathVariable("id") @Parameter(description = "生产需求ID") Long id) {
        return Result.success(productionDemandService.DetailProductionDemand(id));
    }


}

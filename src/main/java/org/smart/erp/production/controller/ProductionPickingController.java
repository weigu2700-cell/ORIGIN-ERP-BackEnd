package org.smart.erp.production.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.production.dto.ProductionPickingPageDto;
import org.smart.erp.production.service.ProductionPickingService;
import org.smart.erp.production.vo.ProductionPickingVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prd/picking")
@Tag(name = "领料单管理", description = "生产订单领料单的查询与确认")
public class ProductionPickingController {

    private final ProductionPickingService productionPickingService;

    public ProductionPickingController(ProductionPickingService productionPickingService) {
        this.productionPickingService = productionPickingService;
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyAuthority('production:picking:confirm')")
    @Operation(summary = "确认领料", description = "库存出库并将领料单状态置为已领料；若同订单全部领完则下达生产")
    public void confirmPicking(@RequestParam @Parameter(description = "领料单ID") Long id) {
        productionPickingService.confirmPicking(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('production:picking:list')")
    @Operation(summary = "分页查询领料单", description = "按生产订单、物料、仓库、状态、领料时间等条件分页查询")
    public Result<Page<ProductionPickingVo>> page(
            @ModelAttribute @Parameter(description = "领料单分页查询条件") ProductionPickingPageDto dto) {
        return Result.success(productionPickingService.pageProductionPicking(dto));
    }
}

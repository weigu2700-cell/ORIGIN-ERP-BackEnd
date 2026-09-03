package org.smart.erp.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.sales.dto.salesDeliveryDto.CreateDto;
import org.smart.erp.sales.dto.salesDeliveryDto.ListDto;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.sales.vo.SalesDeliveryVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales/delivery")
@Tag(name = "销售出库单", description = "销售出库单的创建、编辑、查询、删除与状态流转（确认/取消）")
public class SalesDeliveryController {

    private final SalesDeliveryService salesDeliveryService;

    public SalesDeliveryController(SalesDeliveryService salesDeliveryService) {
        this.salesDeliveryService = salesDeliveryService;
    }

    @Operation(summary = "新增销售出库单",
            description = "基于已确认的销售订单创建草稿态发货单；明细必须归属该订单，客户与订单号自动带出")
    @PreAuthorize("hasAnyAuthority('sales:delivery:create')")
    @PostMapping
    public Result<SalesDeliveryVo> create(@RequestBody @Validated CreateDto dto) {
        return Result.success(salesDeliveryService.createSalesDeliveryVo(dto));
    }

    @Operation(summary = "销售出库单列表", description = "销售出库单列表")
    @PreAuthorize("hasAnyAuthority('sales:delivery:list')")
    @GetMapping
    public Result<Page<SalesDeliveryVo>> list(@Parameter(description = "销售出库单列表") ListDto dto) {
        return Result.success(salesDeliveryService.getPageSalesDeliveryVo(dto));
    }

    @Operation(summary = "销售出库单详情", description = "返回发货单主体及全部明细行")
    @PreAuthorize("hasAnyAuthority('sales:delivery:get')")
    @GetMapping("/{id}")
    public Result<SalesDeliveryVo> get(
            @Parameter(description = "发货单id", required = true)
            @PathVariable Long id) {
        return Result.success(salesDeliveryService.getSalesDeliveryVoById(id));
    }

    @Operation(summary = "确认销售出库单",
            description = "仅草稿态发货单可确认；确认时逐行出库扣减库存（在库与预占同步减少），任一行不足则整体回滚")
    @PreAuthorize("hasAnyAuthority('sales:delivery:confirm')")
    @PutMapping("/{id}/confirm")
    public Result<SalesDeliveryVo> confirm(
            @Parameter(description = "发货单id", required = true)
            @PathVariable Long id) {
        return Result.success(salesDeliveryService.confirmSalesDeliveryById(id));
    }

    @Operation(summary = "取消销售出库单", description = "仅草稿态发货单可取消")
    @PreAuthorize("hasAnyAuthority('sales:delivery:cancel')")
    @PutMapping("/{id}/cancel")
    public Result<SalesDeliveryVo> cancel(
            @Parameter(description = "发货单id", required = true)
            @PathVariable Long id) {
        return Result.success(salesDeliveryService.cancelSalesDeliveryById(id));
    }

}

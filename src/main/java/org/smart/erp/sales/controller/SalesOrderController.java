package org.smart.erp.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
import org.smart.erp.sales.dto.salesOrderDto.listDto;
import org.smart.erp.sales.dto.salesOrderDto.updateDto;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales/order")
@Tag(name = "销售订单", description = "销售订单的创建、编辑、查询、删除与状态流转（确认/取消）")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @Operation(summary = "新增销售订单",
            description = "创建草稿态订单并同步生成明细；明细项不能为空，物料与仓库必须存在且为启用状态")
    @PreAuthorize("hasAnyAuthority('sales:order:create')")
    @PostMapping
    public Result<SalesOrderVo> create(@RequestBody @Validated createDto dto) {
        return Result.success(salesOrderService.create(dto));
    }

    @Operation(summary = "销售订单分页列表",
            description = "支持按客户、订单号、状态、下单时间起过滤")
    @PreAuthorize("hasAnyAuthority('sales:order:list')")
    @GetMapping
    public Result<Page<SalesOrderVo>> list(listDto dto) {
        return Result.success(salesOrderService.listSalesOrderVoByPage(dto));
    }

    @Operation(summary = "销售订单详情", description = "返回订单主体及全部明细行")
    @PreAuthorize("hasAnyAuthority('sales:order:get')")
    @GetMapping("/{id}")
    public Result<SalesOrderVo> get(
            @Parameter(description = "销售订单id", required = true)
            @PathVariable Long id) {
        return Result.success(salesOrderService.getSalesOrderVoById(id));
    }

    @Operation(summary = "修改销售订单",
            description = "修改备注、交货日期与明细；明细按传入项全量替换（未出现的行将被删除），并自动重算订单总金额")
    @PreAuthorize("hasAnyAuthority('sales:order:update')")
    @PutMapping("/{id}")
    public Result<SalesOrderVo> update(
            @Parameter(description = "销售订单id", required = true)
            @PathVariable Long id,
            @RequestBody @Validated updateDto dto) {
        return Result.success(salesOrderService.updateSalesOrderVoById(id, dto));
    }

    @Operation(summary = "删除销售订单", description = "仅草稿态订单可删除，会一并删除其全部明细")
    @PreAuthorize("hasAnyAuthority('sales:order:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(
            @Parameter(description = "销售订单id", required = true)
            @PathVariable Long id) {
        salesOrderService.removeSalesOrderById(id);
        return Result.success();
    }

    @Operation(summary = "确认销售订单",
            description = "仅草稿态订单可确认；确认时逐行预占库存，任一行可用库存不足则整体回滚并报错")
    @PreAuthorize("hasAnyAuthority('sales:order:confirm')")
    @PutMapping("/{id}/confirm")
    public Result<SalesOrderVo> confirm(
            @Parameter(description = "销售订单id", required = true)
            @PathVariable Long id,
            @RequestBody(required = false) updateDto dto) {
        return Result.success(salesOrderService.confirmSalesOrderById(id, dto));
    }

    @Operation(summary = "取消销售订单",
            description = "仅已确认订单可取消；取消时释放确认阶段预占的库存，释放失败则整体回滚")
    @PreAuthorize("hasAnyAuthority('sales:order:cancel')")
    @PutMapping("/{id}/cancel")
    public Result<SalesOrderVo> cancel(
            @Parameter(description = "销售订单id", required = true)
            @PathVariable Long id,
            @RequestBody(required = false) updateDto dto) {
        return Result.success(salesOrderService.cancelSalesOrderById(id, dto));
    }
}

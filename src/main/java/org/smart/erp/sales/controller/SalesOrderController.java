package org.smart.erp.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
import org.smart.erp.sales.dto.salesOrderDto.listDto;
import org.smart.erp.sales.dto.salesOrderDto.updateDto;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales/order")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }


    @PostMapping
    public Result<SalesOrderVo> create(@RequestBody @Validated createDto dto) {
        return Result.success( salesOrderService.create(dto));
    }

    @GetMapping
    public Result<Page<SalesOrderVo>> list(@RequestParam listDto dto) {
        return Result.success(salesOrderService.listSalesOrderVoByPage(dto));
    }

    @GetMapping("/{id}")
    public Result<SalesOrderVo> get(@PathVariable Long id) {
        return Result.success(salesOrderService.getSalesOrderVoById(id));
    }

    @PutMapping("/{id}")
    public Result<SalesOrderVo> update(@PathVariable Long id, @RequestBody @Validated updateDto dto) {
        return Result.success(salesOrderService.updateSalesOrderVoById(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        salesOrderService.removeSalesOrderById(id);
        return Result.success();
    }

    @PutMapping("/{id}/confirm")
    public Result<SalesOrderVo> confirm(@PathVariable Long id, updateDto dto) {
        return Result.success(salesOrderService.confirmSalesOrderById(id,dto));
    }

    @PutMapping("/{id}/cancel")
    public Result<SalesOrderVo> cancel(@PathVariable Long id, updateDto dto) {
        return Result.success(salesOrderService.cancelSalesOrderById(id,dto));
    }
}

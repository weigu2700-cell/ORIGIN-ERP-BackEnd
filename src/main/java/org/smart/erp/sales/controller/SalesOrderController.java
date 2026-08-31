package org.smart.erp.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
import org.smart.erp.sales.dto.salesOrderDto.listDto;
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
}

package org.smart.erp.sales.controller;

import org.smart.erp.common.result.Result;
import org.smart.erp.sales.dto.salesOrderDto.createDto;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

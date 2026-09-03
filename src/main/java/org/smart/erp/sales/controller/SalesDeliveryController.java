package org.smart.erp.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.sales.dto.salesDeliveryDto.ListDto;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.sales.vo.SalesDeliveryVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/delivery")
@Tag(name = "销售出库单", description = "销售出库单的创建、编辑、查询、删除与状态流转（确认/取消）")
public class SalesDeliveryController {

    private final SalesDeliveryService salesDeliveryService;

    public SalesDeliveryController(SalesDeliveryService salesDeliveryService) {
        this.salesDeliveryService = salesDeliveryService;
    }

    @Operation(summary = "销售出库单列表", description = "销售出库单列表")
    @GetMapping
    public Result<Page<SalesDeliveryVo>> list(@Parameter(description = "销售出库单列表") ListDto dto) {
        return Result.success(salesDeliveryService.getPageSalesDeliveryVo(dto));
    }

}

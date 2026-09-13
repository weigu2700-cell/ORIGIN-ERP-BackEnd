package org.smart.erp.production.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.production.dto.ProductionReportAddDto;
import org.smart.erp.production.dto.ProductionReportPageDto;
import org.smart.erp.production.service.ProductionReportService;
import org.smart.erp.production.vo.ProductionReportVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prd/report")
@Tag(name = "生产报工", description = "生产报工相关接口")
public class ProductionReportController {

    private final ProductionReportService productionReportService;

    public ProductionReportController(ProductionReportService productionReportService) {
        this.productionReportService = productionReportService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('prd:report:add')")
    @Operation(summary = "新增报工单")
    public Result<Void> add(
            @RequestBody @Validated @Parameter(description = "新增报工单请求参数") ProductionReportAddDto dto) {
        productionReportService.addProductionReport(dto);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('prd:report:page')")
    @Operation(summary = "分页查询报工单")
    public Result<Page<ProductionReportVo>> page(
            @Parameter(description = "分页查询报工单请求参数") ProductionReportPageDto dto
    ) {
        return Result.success(productionReportService.pageProductionReport(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('prd:report:get')")
    @Operation(summary = "获取报工单详情")
    public Result<ProductionReportVo> detail(
            @PathVariable @Parameter(description = "报工单ID") Long id
    ) {
        return Result.success(productionReportService.detailProductionReport(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('prd:report:approve')")
    @Operation(summary = "审批报工单")
    public Result<Void> approve(@PathVariable @Parameter(description = "报工单ID") Long id) {
        productionReportService.approveProductionReport(id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('prd:report:cancel')")
    @Operation(summary = "取消报工单")
    public Result<Void> cancel(@PathVariable @Parameter(description = "报工单ID") Long id) {
        productionReportService.cancelProductionReport(id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('prd:report:reject')")
    @Operation(summary = "驳回报工单")
    public Result<Void> reject(@PathVariable @Parameter(description = "报工单ID") Long id) {
        productionReportService.rejectProductionReport(id);
        return Result.success();
    }

    @PutMapping("/{id}/finish")
    @PreAuthorize("hasAnyAuthority('prd:report:finish')")
    @Operation(summary = "完成报工单")
    public Result<Void> finish(@PathVariable @Parameter(description = "报工单ID") Long id) {
        productionReportService.finishProductionReport(id);
        return Result.success();
    }
}

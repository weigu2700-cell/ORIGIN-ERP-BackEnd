package org.smart.erp.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.inventory.dto.FinishWarehousingPageDto;
import org.smart.erp.inventory.service.FinishWarehousingService;
import org.smart.erp.inventory.vo.FinishWarehousingVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@RestController
@RequestMapping("/inv/finish-warehousing")
@Tag(name = "成品入库", description = "成品入库相关接口")
public class FinishWarehousingController {

    private final FinishWarehousingService finishWarehousingService;

    public FinishWarehousingController(FinishWarehousingService finishWarehousingService) {
        this.finishWarehousingService = finishWarehousingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:list')")
    @Operation(summary = "分页获取成品入库单", description = "分页获取成品入库单")
    public Result<Page<FinishWarehousingVo>> page(
            @Parameter(name = "分页获取成品入库单参数") FinishWarehousingPageDto dto
    ){
        return Result.success(finishWarehousingService.pageFinishWarehousing(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:get')")
    @Operation(summary = "获取成品入库单详情", description = "获取成品入库单详情")
    public Result<FinishWarehousingVo> get(
            @Parameter(name = "成品入库单 ID") @PathVariable Long id
    ){
        return Result.success(finishWarehousingService.getFinishWarehousing(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:update')")
    @Operation(summary = "审批成品入库单", description = "审批成品入库单")
    public Result<Boolean> approve(
            @Parameter(name = "成品入库单 ID") @PathVariable Long id
    ) {
        return Result.success(finishWarehousingService.approveFinishWarehousing(id));
    }

    @PutMapping("/{id}/warehouse")
    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:update')")
    @Operation(summary = "成品入库", description = "将已审批的入库单执行入库")
    public Result<Boolean> warehouse(
            @Parameter(name = "成品入库单 ID") @PathVariable Long id
    ) {
        return Result.success(finishWarehousingService.warehouseFinishWarehousing(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('inv:finish-warehousing:update')")
    @Operation(summary = "取消成品入库单", description = "取消草稿状态的入库单")
    public Result<Boolean> cancel(
            @Parameter(name = "成品入库单 ID") @PathVariable Long id
    ) {
        return Result.success(finishWarehousingService.cancelFinishWarehousing(id));
    }
}
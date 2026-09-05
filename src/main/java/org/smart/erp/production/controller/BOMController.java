package org.smart.erp.production.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.production.dto.creatBOMDto;
import org.smart.erp.production.dto.pageBOMDto;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.vo.BOMVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("prd/bom")
@Tag(name = "BOM管理", description = "物料清单(BOM)的详情查询与分页列表")
public class BOMController {

    private final BOMService bomService;

    public BOMController(BOMService bomService) {
        this.bomService = bomService;
    }


    @PostMapping
    @Operation(summary = "创建BOM", description = "创建BOM")
    public Result<Void> createBOM(@RequestBody @Validated @Parameter(description = "创建BOM参数") creatBOMDto dto) {
        bomService.createBOM(dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "BOM详情", description = "根据 BOM 主键查询其头信息及组成明细")
    public Result<BOMVo> getBOMById(@PathVariable @Parameter(description = "BOM 主键 ID") Long id) {
        return Result.success(bomService.getBOMDetailById(id));
    }

    @GetMapping
    @Operation(summary = "BOM分页列表", description = "按 BOM 单号 / 物料 / 状态条件分页查询")
    public Result<Page<BOMVo>> getPage(pageBOMDto dto) {
        return Result.success(bomService.getPageBOMVo(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改BOM", description = "修改BOM")
    public Result<Void> activeBOM(@PathVariable @Parameter(description = "BOM 主键 ID") Long id) {
        bomService.activeBOM(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用BOM", description = "禁用BOM")
    public Result<Void> disableBOM(@PathVariable @Parameter(description = "BOM 主键 ID") Long id) {
        bomService.disableBOM(id);
        return Result.success();
    }
}

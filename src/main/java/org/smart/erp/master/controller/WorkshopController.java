package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.WorkshopDto.WorkshopAddDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopPageDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopStatusChangeDto;
import org.smart.erp.master.dto.WorkshopDto.WorkshopUpdateDto;
import org.smart.erp.master.service.WorkshopService;
import org.smart.erp.master.vo.WorkshopVo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("master/workshop")
@Tag(name = "车间管理", description = "车间的增删改查、详情与状态变更")
public class WorkshopController {

    private final WorkshopService workshopService;

    public WorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @Operation(summary = "新增车间")
    @PostMapping
    public Result<Void> addWorkshop(@RequestBody WorkshopAddDto dto) {
        workshopService.addWorkshop(dto);
        return Result.success();
    }

    @Operation(summary = "车间分页列表")
    @GetMapping
    public Result<Page<WorkshopVo>> pageWorkshop(WorkshopPageDto dto) {
        return Result.success(workshopService.pageWorkshop(dto));
    }

    @Operation(summary = "车间详情")
    @GetMapping("/{id}")
    public Result<WorkshopVo> detailWorkshop(@PathVariable Long id) {
        return Result.success(workshopService.detailWorkshop(id));
    }

    @Operation(summary = "更新车间")
    @PutMapping("/{id}")
    public Result<Void> updateWorkshop(@PathVariable Long id, @RequestBody WorkshopUpdateDto dto) {
        workshopService.updateWorkshop(id, dto);
        return Result.success();
    }

    @Operation(summary = "变更车间状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody WorkshopStatusChangeDto dto) {
        workshopService.changeStatus(id, dto.getStatus());
        return Result.success();
    }
}

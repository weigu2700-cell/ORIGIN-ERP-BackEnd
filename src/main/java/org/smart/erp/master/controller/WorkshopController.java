package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopCreateDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopListDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopStatusChangeDTO;
import org.smart.erp.master.dto.WorkshopDTO.WorkshopUpdateDTO;
import org.smart.erp.master.service.WorkshopService;
import org.smart.erp.master.vo.WorkshopVO;
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
    public Result<Void> createWorkshop(@RequestBody WorkshopCreateDTO dto) {
        workshopService.createWorkshop(dto);
        return Result.success();
    }

    @Operation(summary = "车间分页列表")
    @GetMapping
    public Result<Page<WorkshopVO>> listWorkshop(WorkshopListDTO dto) {
        return Result.success(workshopService.listWorkshop(dto));
    }

    @Operation(summary = "车间详情")
    @GetMapping("/{id}")
    public Result<WorkshopVO> getWorkshopDetail(@PathVariable Long id) {
        return Result.success(workshopService.getWorkshopDetail(id));
    }

    @Operation(summary = "更新车间")
    @PutMapping("/{id}")
    public Result<Void> updateWorkshop(@PathVariable Long id, @RequestBody WorkshopUpdateDTO dto) {
        workshopService.updateWorkshop(id, dto);
        return Result.success();
    }

    @Operation(summary = "变更车间状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody WorkshopStatusChangeDTO dto) {
        workshopService.changeStatus(id, dto.getStatus());
        return Result.success();
    }
}

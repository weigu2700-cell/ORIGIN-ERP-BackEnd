package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.WorkshopCreateDTO;
import org.smart.erp.master.dto.WorkshopListDTO;
import org.smart.erp.master.dto.WorkshopUpdateDTO;
import org.smart.erp.master.enums.WorkshopStatus;
import org.smart.erp.master.service.WorkshopService;
import org.smart.erp.master.vo.WorkshopVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("master/workshop")
public class WorkshopController {

    private final WorkshopService workshopService;

    public WorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @PostMapping
    public Result<Void> createWorkshop(WorkshopCreateDTO dto) {
        workshopService.createWorkshop(dto);
        return Result.success();
    }

    @GetMapping
    public Result<Page<WorkshopVO>> listWorkshop(WorkshopListDTO dto) {
        return Result.success(workshopService.listWorkshop(dto));
    }

    @GetMapping("/{id}")
    public Result<WorkshopVO> getWorkshop(@PathVariable Long id) {
        return Result.success(workshopService.getWorkshopDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateWorkshop(@PathVariable Long id, WorkshopUpdateDTO dto) {
        workshopService.updateWorkshop(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, WorkshopStatus status) {
        workshopService.changeStatus(id, status);
        return Result.success();
    }
}

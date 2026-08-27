package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.FactoryDTO.FactoryCreateDTO;
import org.smart.erp.master.dto.FactoryDTO.FactoryListDTO;
import org.smart.erp.master.dto.FactoryDTO.FactoryUpdateDTO;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.service.FactoryService;
import org.smart.erp.master.vo.FactoryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("master/factory")
@Tag(name = "工厂管理", description = "工厂的增删改查、详情与状态变更")
public class FactoryController {

    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    @Operation(summary = "新增工厂")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('factory:create')")
    public Result<Void> createFactory(@RequestBody FactoryCreateDTO dto) {
        factoryService.createFactory(dto);
        return Result.success();
    }

    @Operation(summary = "工厂分页列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('factory:list')")
    public Result<Page<FactoryVO>> listFactory(FactoryListDTO dto) {
        return Result.success(factoryService.getFactoryList(dto));
    }

    @Operation(summary = "更新工厂")
    @PutMapping("{id}")
    @PreAuthorize("hasAnyAuthority('factory:update')")
    public Result<Void> updateFactory(@PathVariable Long id, @RequestBody FactoryUpdateDTO dto) {
        factoryService.updateFactory(id, dto);
        return Result.success();
    }

    @Operation(summary = "工厂详情")
    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('factory:get')")
    public Result<FactoryVO> getFactoryDetail(@PathVariable Long id) {
        return Result.success(factoryService.getFactoryById(id));
    }

    @Operation(summary = "变更工厂状态")
    @PutMapping("{id}/status")
    @PreAuthorize("hasAnyAuthority('factory:status:update')")
    public Result<Void> updateFactoryStatus(@PathVariable Long id, @RequestParam FactoryStatus status) {
        factoryService.updateFactoryStatus(id, status);
        return Result.success();
    }
}

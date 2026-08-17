package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Update;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.FactoryCreateDTO;
import org.smart.erp.master.dto.FactoryListDTO;
import org.smart.erp.master.dto.FactoryUpdateDTO;
import org.smart.erp.master.entity.Factory;
import org.smart.erp.master.enums.FactoryStatus;
import org.smart.erp.master.service.FactoryService;
import org.smart.erp.master.vo.FactoryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("master/factory")
public class FactoryController {

    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('factory:create')")
    public Result<Void> createFactory(@RequestBody FactoryCreateDTO dto) {
        factoryService.createFactory(dto);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('factory:list')")
    public Result<Page<FactoryVO>> getFactoryList(@RequestParam FactoryListDTO dto) {
        return Result.success(factoryService.getFactoryList(dto));
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyAuthority('factory:update')")
    public Result<Void> updateFactory(@PathVariable Long id, @RequestBody FactoryUpdateDTO dto) {
        factoryService.updateFactory(id, dto);
        return Result.success();
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('factory:get')")
    public Result<FactoryVO> getFactory(@PathVariable Long id) {
        return Result.success(factoryService.getFactoryById(id));
    }

    @PutMapping("{id}/status")
    @PreAuthorize("hasAnyAuthority('factory:status:update')")
    public Result<Void> updateFactoryStatus(@PathVariable Long id, @RequestParam FactoryStatus status) {
        factoryService.updateFactoryStatus(id, status);
        return Result.success();
    }
}

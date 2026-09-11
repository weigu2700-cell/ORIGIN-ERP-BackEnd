package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.DeptDto;
import org.smart.erp.system.dto.DeptListDto;
import org.smart.erp.system.dto.DeptUpdateDto;
import org.smart.erp.system.service.DeptService;
import org.smart.erp.system.vo.DeptTreeVo;
import org.smart.erp.system.vo.DeptVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dept")
@Tag(name = "部门管理", description = "部门的增删改查、列表与树形结构")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @Operation(summary = "新增部门")
    @PostMapping("/add")
    public Result<Void> addDept(@RequestBody DeptDto dto) {
        deptService.addDept(dto);
        return Result.success();
    }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public Result<DeptVo> detailDept(@PathVariable Long id) {
        return Result.success(deptService.detailDept(id));
    }

    @Operation(summary = "部门分页列表")
    @GetMapping("/list")
    public Result<Page<DeptVo>> pageDept(DeptListDto dto) {
        return Result.success(deptService.pageDept(dto));
    }

    @Operation(summary = "部门树形结构")
    @GetMapping("/tree")
    public Result<List<DeptTreeVo>> getDeptTree() {
        return Result.success(deptService.getDeptTree());
    }

    @Operation(summary = "更新部门")
    @PutMapping()
    public Result<Void> updateDept(@RequestBody DeptUpdateDto dto) {
        deptService.updateDept(dto);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public Result<Boolean> removeDept(@PathVariable Long id) {
        return Result.success(deptService.removeDept(id));
    }
}

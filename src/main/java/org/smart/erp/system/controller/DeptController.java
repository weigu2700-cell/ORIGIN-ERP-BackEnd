package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.DeptDTO;
import org.smart.erp.system.dto.DeptListDTO;
import org.smart.erp.system.dto.DeptUpdateDTO;
import org.smart.erp.system.service.DeptService;
import org.smart.erp.system.vo.DeptTreeVO;
import org.smart.erp.system.vo.DeptVO;
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
    public Result<Void> createDept(@RequestBody DeptDTO dto) {
        deptService.createDept(dto);
        return Result.success();
    }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public Result<DeptVO> getDeptDetail(@PathVariable Long id) {
        return Result.success(deptService.getDeptDetail(id));
    }

    @Operation(summary = "部门分页列表")
    @GetMapping("/list")
    public Result<Page<DeptVO>> listDept(DeptListDTO dto) {
        return Result.success(deptService.listDept(dto));
    }

    @Operation(summary = "部门树形结构")
    @GetMapping("/tree")
    public Result<List<DeptTreeVO>> getDeptTree() {
        return Result.success(deptService.getDeptTree());
    }

    @Operation(summary = "更新部门")
    @PutMapping()
    public Result<Void> updateDept(@RequestBody DeptUpdateDTO dto) {
        deptService.updateDept(dto);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteDept(@PathVariable Long id) {
        return Result.success(deptService.deleteDept(id));
    }
}

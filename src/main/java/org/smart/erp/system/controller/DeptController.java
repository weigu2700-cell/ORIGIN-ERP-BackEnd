package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.DeptDTO;
import org.smart.erp.system.dto.DeptListDTO;
import org.smart.erp.system.dto.DeptUpdateDTO;
import org.smart.erp.system.service.DeptService;
import org.smart.erp.system.vo.DeptVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/dept")
@Valid
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @PostMapping("/add")
    public Result<DeptVO> add(@RequestBody DeptDTO dto) {
        return Result.success(deptService.createDept(dto));
    }

    @GetMapping("/{id}")
    public Result<DeptVO> getDeptDetail(@PathVariable Long id) {
        return Result.success(deptService.getDeptDetail(id));
    }

    @GetMapping("/list")
    public Result<Page<DeptVO>> getDeptList(DeptListDTO dto) {
        return Result.success(deptService.getDeptList(dto));
    }

    @PutMapping()
    public Result<DeptVO> updateDept(@RequestBody DeptUpdateDTO dto) {
        return Result.success(deptService.updateDept(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteDept(@PathVariable Long id) {
        return Result.success(deptService.deleteDept(id));
    }
}

package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.CustomerDto.CustomerAddDto;
import org.smart.erp.master.dto.CustomerDto.CustomerPageDto;
import org.smart.erp.master.dto.CustomerDto.CustomerStatusDto;
import org.smart.erp.master.dto.CustomerDto.CustomerUpdateDto;
import org.smart.erp.master.service.CustomerService;
import org.smart.erp.master.vo.CustomerVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master/customer")
@RequiredArgsConstructor
@Tag(name = "客户管理", description = "客户的增删改查")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "新增客户")
    @PostMapping
    @PreAuthorize("hasAuthority('master:customer:create')")
    public Result<Void> add(@Valid @RequestBody CustomerAddDto dto) {
        customerService.addCustomer(dto);
        return Result.success();
    }

    @Operation(summary = "客户分页列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('master:customer:list')")
    public Result<Page<CustomerVo>> pageCustomer(CustomerPageDto dto) {
        return Result.success(customerService.getCustomerList(dto));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:customer:get')")
    public Result<CustomerVo> detailCustomer(@PathVariable Long id) {
        return Result.success(customerService.detailCustomer(id));
    }

    @Operation(summary = "更新客户")
    @PutMapping
    @PreAuthorize("hasAuthority('master:customer:update')")
    public Result<CustomerVo> update(@Valid @RequestBody CustomerUpdateDto dto) {
        return Result.success(customerService.updateCustomer(dto));
    }

    @Operation(summary = "更改客户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:customer:status')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody CustomerStatusDto dto) {
        customerService.changeCustomerStatus(id, dto);
        return Result.success();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('master:customer:export')")
    public void exportCustomer(@RequestParam(required = false) List<Long> ids, HttpServletResponse response) {
        customerService.exportCustomer(ids, response);
    }
}

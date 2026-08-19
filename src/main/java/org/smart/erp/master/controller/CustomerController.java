package org.smart.erp.master.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.CustomerDTO.CustomerCreateDTO;
import org.smart.erp.master.dto.CustomerDTO.CustomerListDTO;
import org.smart.erp.master.dto.CustomerDTO.CustomerStatusDTO;
import org.smart.erp.master.dto.CustomerDTO.CustomerUpdateDTO;
import org.smart.erp.master.service.CustomerService;
import org.smart.erp.master.vo.CustomerVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/master/customer")
@RequiredArgsConstructor
@Tag(name = "客户管理", description = "客户的增删改查")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "新增客户")
    @PostMapping
    @PreAuthorize("hasAuthority('master:customer:create')")
    public Result<Void> create(@Valid @RequestBody CustomerCreateDTO dto) {
        customerService.createCustomer(dto);
        return Result.success();
    }

    @Operation(summary = "客户分页列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('master:customer:list')")
    public Result<Page<CustomerVO>> listCustomer(CustomerListDTO dto) {
        return Result.success(customerService.getCustomerList(dto));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:customer:get')")
    public Result<CustomerVO> getCustomerDetail(@PathVariable Long id) {
        return Result.success(customerService.getCustomerDetail(id));
    }

    @Operation(summary = "更新客户")
    @PutMapping
    @PreAuthorize("hasAuthority('master:customer:update')")
    public Result<CustomerVO> update(@Valid @RequestBody CustomerUpdateDTO dto) {
        return Result.success(customerService.updateCustomer(dto));
    }

    @Operation(summary = "更改客户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:customer:status')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody CustomerStatusDTO dto) {
        customerService.changeCustomerStatus(id, dto);
        return Result.success();
    }
}

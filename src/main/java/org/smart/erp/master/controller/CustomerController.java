package org.smart.erp.master.controller;

import lombok.RequiredArgsConstructor;
import org.smart.erp.common.result.Result;
import org.smart.erp.master.dto.CustomerCreateDTO;
import org.smart.erp.master.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('master:customer:create')")
    public Result<Void> create(@RequestBody CustomerCreateDTO dto) {
        customerService.createCustomer(dto);
        return Result.success();
    }
}

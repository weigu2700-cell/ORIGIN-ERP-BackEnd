package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.CustomerDto.CustomerAddDto;
import org.smart.erp.master.dto.CustomerDto.CustomerPageDto;
import org.smart.erp.master.dto.CustomerDto.CustomerStatusDto;
import org.smart.erp.master.dto.CustomerDto.CustomerUpdateDto;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.vo.CustomerVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface CustomerService extends IService<Customer> {
    @PreAuthorize("hasAuthority('master:customer:create')")
    void addCustomer(CustomerAddDto dto);

    @PreAuthorize("hasAuthority('master:customer:list')")
    Page<CustomerVo> getCustomerList(CustomerPageDto dto);

    @PreAuthorize("hasAuthority('master:customer:get')")
    CustomerVo detailCustomer(Long id);

    @PreAuthorize("hasAuthority('master:customer:update')")
    CustomerVo updateCustomer(CustomerUpdateDto dto);

    @PreAuthorize("hasAuthority('master:customer:status')")
    void changeCustomerStatus(Long id, CustomerStatusDto dto);

    @PreAuthorize("hasAuthority('master:customer:export')")
    void exportCustomer(List<Long> ids, HttpServletResponse response);
}

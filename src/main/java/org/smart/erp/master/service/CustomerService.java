package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.CustomerDto.CustomerAddDto;
import org.smart.erp.master.dto.CustomerDto.CustomerPageDto;
import org.smart.erp.master.dto.CustomerDto.CustomerStatusDto;
import org.smart.erp.master.dto.CustomerDto.CustomerUpdateDto;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.vo.CustomerVo;

import java.util.List;

public interface CustomerService extends IService<Customer> {
    void addCustomer(CustomerAddDto dto);

    Page<CustomerVo> getCustomerList(CustomerPageDto dto);

    CustomerVo detailCustomer(Long id);

    CustomerVo updateCustomer(CustomerUpdateDto dto);

    void changeCustomerStatus(Long id, CustomerStatusDto dto);

    void exportCustomer(List<Long> ids, HttpServletResponse response);
}

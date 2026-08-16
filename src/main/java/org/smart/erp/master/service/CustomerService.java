package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.CustomerCreateDTO;
import org.smart.erp.master.entity.Customer;

public interface CustomerService extends IService<Customer> {
    void createCustomer(CustomerCreateDTO dto);
}

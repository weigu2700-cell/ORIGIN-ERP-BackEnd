package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.CustomerCreateDTO;
import org.smart.erp.master.dto.CustomerListDTO;
import org.smart.erp.master.dto.CustomerStatusDTO;
import org.smart.erp.master.dto.CustomerUpdateDTO;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.vo.CustomerVO;

public interface CustomerService extends IService<Customer> {
    void createCustomer(CustomerCreateDTO dto);

    Page<CustomerVO> getCustomerList(CustomerListDTO dto);

    CustomerVO getCustomerDetail(Long id);

    CustomerVO updateCustomer(CustomerUpdateDTO dto);

    void changeCustomerStatus(Long id, CustomerStatusDTO dto);
}

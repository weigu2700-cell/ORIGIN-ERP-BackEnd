package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.SnowflakeIdGenerator;
import org.smart.erp.master.dto.CustomerCreateDTO;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.enums.CustomerStatus;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.master.service.CustomerService;
import org.smart.erp.master.vo.CustomerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator();

    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        return vo;
    }

    @Override
    public void createCustomer(CustomerCreateDTO dto) {
        Customer customer = new Customer();
        Customer isExist = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getName, dto.getName())
        );
        if (isExist != null) {
            throw new BusinessException(400, "客户已存在");
        }
        BeanUtils.copyProperties(dto, customer);
        String customerCode = "CU"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + SNOWFLAKE.nextIdSuffix(4);

        customer.setCode(customerCode);
        customer.setStatus(CustomerStatus.ACTIVE);
        this.save(customer);
    }
}

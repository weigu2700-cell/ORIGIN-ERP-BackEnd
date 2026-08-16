package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.SnowflakeIdGenerator;
import org.smart.erp.master.dto.CustomerCreateDTO;
import org.smart.erp.master.dto.CustomerListDTO;
import org.smart.erp.master.dto.CustomerStatusDTO;
import org.smart.erp.master.dto.CustomerUpdateDTO;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.enums.CustomerStatus;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.master.service.CustomerService;
import org.smart.erp.master.vo.CustomerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;



@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator();

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        if (customer.getStatus() != null) {
            vo.setStatus(customer.getStatus().getCode());
        }
        vo.setCreatedTime(customer.getCreateTime());
        return vo;
    }

    private void applyUpdate(Customer customer, CustomerUpdateDTO dto) {
        if (dto.getName() != null) customer.setName(dto.getName());
        if (dto.getShortName() != null) customer.setShortName(dto.getShortName());
        if (dto.getContactName() != null) customer.setContactName(dto.getContactName());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getEmail() != null) customer.setEmail(dto.getEmail());
        if (dto.getRemark() != null) customer.setRemark(dto.getRemark());
    }

    @Override
    public void createCustomer(CustomerCreateDTO dto) {
        if (StringUtils.hasText(dto.getPhone())) {
            Long count = this.lambdaQuery()
                    .eq(Customer::getPhone, dto.getPhone())
                    .count();
            if (count != null && count > 0) {
                throw new BusinessException(409, "该联系电话已存在");
            }
        }
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        customer.setCode("CU"  + SNOWFLAKE);
        customer.setStatus(CustomerStatus.ACTIVE);
        this.save(customer);
    }

    @Override
    public Page<CustomerVO> getCustomerList(CustomerListDTO dto) {
        LambdaQueryWrapper<Customer> wrapper =
                new LambdaQueryWrapper<Customer>()
                    .eq(StringUtils.hasText(dto.getCode()), Customer::getCode, dto.getCode())
                    .like(StringUtils.hasText(dto.getName()), Customer::getName, dto.getName())
                    .eq(dto.getStatus() != null, Customer::getStatus, dto.getStatus())
                    .orderByDesc(Customer::getCreateTime);

        Page<Customer> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), wrapper);

        Page<CustomerVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public CustomerVO getCustomerDetail(Long id) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return this.toVO(customer);
    }

    @Override
    public CustomerVO updateCustomer(CustomerUpdateDTO dto) {
        Customer customer = this.getById(dto.getId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        applyUpdate(customer, dto);
        this.updateById(customer);
        return this.toVO(customer);
    }

    @Override
    public void changeCustomerStatus(Long id, CustomerStatusDTO dto) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        customer.setStatus(dto.getStatus());
        this.updateById(customer);
    }
}

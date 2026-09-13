package org.smart.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.common.utils.SnowflakeIdGenerator;
import org.smart.erp.master.convertor.ApplyUpdate;
import org.smart.erp.master.dto.CustomerDto.CustomerAddDto;
import org.smart.erp.master.dto.CustomerDto.CustomerPageDto;
import org.smart.erp.master.dto.CustomerDto.CustomerStatusDto;
import org.smart.erp.master.dto.CustomerDto.CustomerUpdateDto;
import org.smart.erp.master.entity.Customer;
import org.smart.erp.master.enums.CustomerStatus;
import org.smart.erp.master.mapper.CustomerMapper;
import org.smart.erp.master.service.CustomerService;
import org.smart.erp.master.vo.CustomerVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.idev.excel.FastExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.master.vo.CustomerExportVo;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private static final SnowflakeIdGenerator SNOWFLAKE = new SnowflakeIdGenerator();

    private CustomerVo toVO(Customer customer) {
        CustomerVo vo = new CustomerVo();
        BeanUtils.copyProperties(customer, vo);
        if (customer.getStatus() != null) {
            vo.setStatus(customer.getStatus().getCode());
        }
        vo.setCreatedTime(customer.getCreateTime());
        return vo;
    }

    @Override
    public void addCustomer(CustomerAddDto dto) {
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
        customer.setCode("CU" + SNOWFLAKE.nextId());
        customer.setStatus(CustomerStatus.ACTIVE);
        this.save(customer);
    }

    @Override
    public Page<CustomerVo> getCustomerList(CustomerPageDto dto) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(StringUtils.hasText(dto.getCode()), Customer::getCode, dto.getCode())
                .like(StringUtils.hasText(dto.getName()), Customer::getName, dto.getName())
                .eq(dto.getStatus() != null, Customer::getStatus, dto.getStatus())
                .orderByDesc(Customer::getCreateTime);

        Page<Customer> page = this.page(new Page<>(dto.getPage(), dto.getPageSize()), wrapper);
        return PageConvertUtils.convert(page, this::toVO);
    }

    @Override
    public CustomerVo detailCustomer(Long id) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return this.toVO(customer);
    }

    @Override
    public CustomerVo updateCustomer(CustomerUpdateDto dto) {
        Customer customer = this.getById(dto.getId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        ApplyUpdate.setUpdateValue(customer, dto);
        this.updateById(customer);
        return this.toVO(customer);
    }

    @Override
    public void changeCustomerStatus(Long id, CustomerStatusDto dto) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        customer.setStatus(dto.getStatus());
        this.updateById(customer);
    }

    @Override
    public void exportCustomer(List<Long> ids, HttpServletResponse response) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(Customer::getId, ids);
        }
        List<Customer> customers = this.list(wrapper);

        List<CustomerExportVo> data = customers.stream()
                .map(customer -> {
                    CustomerExportVo vo = new CustomerExportVo();
                    BeanUtils.copyProperties(customer, vo);
                    if (customer.getStatus() != null) {
                        vo.setStatusDesc(customer.getStatus().getDesc());
                    }
                    return vo;
                })
                .toList();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            String fileName = URLEncoder.encode("客户信息", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=utf-8''" + fileName + ".xlsx");
            FastExcel.write(response.getOutputStream(), CustomerExportVo.class)
                    .sheet("客户信息")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("导出客户信息失败", e);
            throw new BusinessException(500, "导出客户信息失败");
        }
    }
}

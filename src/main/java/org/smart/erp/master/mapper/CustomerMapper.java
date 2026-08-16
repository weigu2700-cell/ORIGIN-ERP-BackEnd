package org.smart.erp.master.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.master.entity.Customer;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}

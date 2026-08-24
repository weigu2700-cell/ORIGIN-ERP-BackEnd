package org.smart.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.smart.erp.inventory.entity.Transaction;

@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {
}

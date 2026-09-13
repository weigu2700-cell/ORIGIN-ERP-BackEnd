package org.smart.erp.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.smart.erp.sales.entity.SalesOrderItem;

import java.math.BigDecimal;

@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {

    /** 原子累加销售订单明细的已发货量（在数据库端完成 +delta，避免并发丢失更新） */
    @Update("UPDATE sal_order_item SET delivered_quantity = delivered_quantity + #{delta} WHERE id = #{id}")
    void increaseDeliveredQuantity(@Param("id") Long id, @Param("delta") BigDecimal delta);
}

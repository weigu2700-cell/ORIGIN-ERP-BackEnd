package org.smart.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sal_delivery_item")
public class SalesDeliveryItem {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long deliveryId;

    private Integer lineNo;

    private Long salesOrderItemId;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal quantity;

}

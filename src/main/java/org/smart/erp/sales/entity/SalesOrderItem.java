package org.smart.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sal_order_item")
public class SalesOrderItem {

    @TableId(value = "id" , type = IdType.ASSIGN_ID)
    private Long id;

    private Long salesOrderId;

    private int lineNo;

    private Long materialId;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Long warehouseId;

    private LocalDateTime deliveryDate;

    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time" , fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package org.smart.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.sales.enums.SalesDeliveryStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@TableName("sal_delivery")
public class SalesDelivery {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String deliveryNo;

    private  Long salesOrderId;

    private String salesOrderNo;

    private Long customerId;

    private LocalDateTime deliveryDate;

    private SalesDeliveryStatus status;

    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

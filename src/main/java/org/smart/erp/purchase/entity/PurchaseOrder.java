package org.smart.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pur_purchase_order")
public class PurchaseOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String purchaseOrderNo;

    private Long purchaseDemandId;

    private Long materialId;

    private Long supplierId;

    private BigDecimal plannedQuantity;

    private BigDecimal completeQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private LocalDateTime orderDate;

    private LocalDateTime expectedDeliveryDate;

    private LocalDateTime actualDeliveryDate;

    private PurchaseOrderStatus status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}

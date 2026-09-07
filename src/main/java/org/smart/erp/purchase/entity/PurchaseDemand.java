package org.smart.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pur_purchase_demand")
public class PurchaseDemand {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String purchaseDemandNo;

    private Long materialId;

    private BigDecimal purchaseQuantity;

    private PurchaseDemandSourceType sourceType;

    private String sourceNo;

    private PurchaseDemandStatus status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

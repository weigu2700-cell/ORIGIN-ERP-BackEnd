package org.smart.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pur_purchase_in_stock")
public class PurchaseInStock {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String inStockNo;

    private Long purchaseOrderId;

    private String purchaseOrderNo;

    private Long materialId;

    private BigDecimal inQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private Long warehouseId;

    private String storageLocation;

    private String batchNo;

    private LocalDateTime productionDate;

    private LocalDateTime expiryDate;

    private PurchaseInStockType inType;

    private PurchaseInStockStatus status;

    private String remark;

    private String operator;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

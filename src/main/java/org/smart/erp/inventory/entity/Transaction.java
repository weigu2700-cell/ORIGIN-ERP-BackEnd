package org.smart.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.smart.erp.inventory.enums.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_transaction")
public class Transaction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal quantity;

    private TransactionType transactionType;

    private String businessType;

    private String businessNo;

    private BigDecimal beforeOnHand;

    private BigDecimal afterOnHand;

    private BigDecimal beforeReserved;

    private BigDecimal afterReserved;

    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}

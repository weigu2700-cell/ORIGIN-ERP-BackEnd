package org.smart.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    /** 本次确认时已实际预占的数量（缺货时为部分预占；用于出库时补齐预占、取消时释放） */
    @TableField("reserved_quantity")
    private BigDecimal reservedQuantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

package org.smart.erp.production.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.smart.erp.production.enums.ProductionSourceType;
import org.smart.erp.production.enums.ProductionStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prd_production_demand")
public class ProductionDemand {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String demandNo;

    private ProductionSourceType sourceType;

    private String sourceNo;

    private ProductionStatus status;

    private Long materialId;

    @TableField("demand_quantity")
    private BigDecimal quantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

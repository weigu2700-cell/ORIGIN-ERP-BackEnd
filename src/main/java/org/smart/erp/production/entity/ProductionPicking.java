package org.smart.erp.production.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.smart.erp.production.enums.ProductionPickingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "prd_production_picking")
public class ProductionPicking {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String pickingNo;

    private Long productionOrderId;

    /** 关联采购需求 ID：缺料物料的领料单与采购需求一一对应，在库物料为空 */
    private Long relatedDemandId;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal plannedQuantity;

    private BigDecimal actualQuantity;

    private ProductionPickingStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:SS")
    private LocalDateTime pickingTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:SS")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:SS")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}

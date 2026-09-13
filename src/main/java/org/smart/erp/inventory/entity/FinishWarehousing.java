package org.smart.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.smart.erp.inventory.enums.FinishWarehousingStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_finish_warehousing")
public class FinishWarehousing {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String warehousingNo;

    private Long productionOrderId;

    private Long productionReportId;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal warehousingQuantity;

    private Long warehousingUserId;

    @DateTimeFormat(pattern = "yyy-mm-dd HH:MM:SS")
    private LocalDateTime warehousingTime;

    private FinishWarehousingStatus status;

    private String remark;

    @Version
    private Integer version;

    @DateTimeFormat(pattern = "yyy-mm-dd HH:MM:SS")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyy-mm-dd HH:MM:SS")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

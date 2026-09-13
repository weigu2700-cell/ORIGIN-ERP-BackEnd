package org.smart.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinishWarehousingAddDto {

    @NotNull(message = "生产单ID不能为空")
    private Long productionOrderId;

    @NotNull(message = "生产报工ID不能为空")
    private Long productionReportId;

    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @NotNull(message = "入库数量不能为空")
    private BigDecimal warehousingQuantity;

    @NotNull(message = "入库人ID不能为空")
    private Long warehousingUserId;

    private LocalDateTime warehousingTime;

    private String remark;
}

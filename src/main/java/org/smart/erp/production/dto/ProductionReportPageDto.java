package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.ProductionReportStatus;

import java.time.LocalDateTime;

@Data
public class ProductionReportPageDto {

    private Integer pageNum;

    private Integer pageSize;

    private Long productionOrderId;

    private String productionReportNo;

    private Long materialId;

    private Long reportUserId;

    private LocalDateTime reportTime;

    private ProductionReportStatus status;


}

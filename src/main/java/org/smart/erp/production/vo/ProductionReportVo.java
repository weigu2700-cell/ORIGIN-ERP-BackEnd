package org.smart.erp.production.vo;

import lombok.Data;
import org.smart.erp.production.enums.ProductionReportStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductionReportVo {

    private Long id;

    private String productionReportNo;

    private Long productionOrderId;

    private String productionOrderNo;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private Long reportUserId;

    private String reportUserName;

    private LocalDateTime reportTime;

    private BigDecimal reportQuantity;

    private BigDecimal qualifiedQuantity;

    private BigDecimal scrappedQuantity;

    private ProductionReportStatus status;

    private String remark;

}

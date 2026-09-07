package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;

import java.time.LocalDateTime;

@Data
public class PagePurchaseDemandDto {

    private Integer pageNum;

    private Integer pageSize;

    private Long materialId;

    private PurchaseDemandSourceType sourceType;

    private String sourceNo;

    private PurchaseDemandStatus status;
}

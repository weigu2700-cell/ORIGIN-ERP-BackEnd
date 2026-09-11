package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;

import java.math.BigDecimal;

@Data
public class PurchaseDemandAddDto {

    private Long materialId;

    private PurchaseDemandSourceType sourceType;

    private String sourceNo;

    private BigDecimal purchaseQuantity;
}

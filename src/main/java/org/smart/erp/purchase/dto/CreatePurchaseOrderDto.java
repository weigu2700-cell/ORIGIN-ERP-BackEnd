package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePurchaseOrderDto {

    private Long materialId;

    private Long supplierId;

    private Long purchaseDemandId;

    private BigDecimal unitPrice;

    private BigDecimal plannedQuantity;

    private LocalDateTime expectedDeliveryDate;
}

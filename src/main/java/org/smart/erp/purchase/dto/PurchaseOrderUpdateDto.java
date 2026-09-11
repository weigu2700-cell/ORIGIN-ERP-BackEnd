package org.smart.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderUpdateDto {

    private Long supplierId;

    private BigDecimal unitPrice;

    private BigDecimal plannedQuantity;

    private LocalDateTime expectedDeliveryDate;
}

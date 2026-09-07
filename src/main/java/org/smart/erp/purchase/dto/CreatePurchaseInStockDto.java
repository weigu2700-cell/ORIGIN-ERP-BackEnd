package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseInStockType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePurchaseInStockDto {

    private Long purchaseOrderId;

    private Long supplierId;

    private Long materialId;

    private Long warehouseId;

    private String storageLocation;

    private PurchaseInStockType inType;

    private String remark;

    private BigDecimal inQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private LocalDateTime productionDate;

    private LocalDateTime deliveryDate;

    private LocalDateTime expiryDate;

}

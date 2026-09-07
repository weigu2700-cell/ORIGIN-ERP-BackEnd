package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseInStockType;

import java.time.LocalDateTime;

@Data
public class PagePurchaseInStockDto {

    private Integer pageNum;

    private Integer pageSize;

    private String purchaseOrderNo;

    private Long materialId;

    private Long supplierId;

    private Long warehouseId;

    private String storageLocation;

    private String operator;

    private PurchaseInStockType inType;

    private LocalDateTime productionDate;

    private LocalDateTime deliveryDate;
}

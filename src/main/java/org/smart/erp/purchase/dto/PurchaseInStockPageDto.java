package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;

import java.time.LocalDateTime;

@Data
public class PurchaseInStockPageDto {

    private Integer pageNum;

    private Integer pageSize;

    private String purchaseInStockNo;

    private String purchaseOrderNo;

    private Long materialId;

    private Long supplierId;

    private Long warehouseId;

    private String storageLocation;

    private String operator;

    private PurchaseInStockType inType;

    private PurchaseInStockStatus status;

    private LocalDateTime productionDate;

    private LocalDateTime deliveryDate;
}

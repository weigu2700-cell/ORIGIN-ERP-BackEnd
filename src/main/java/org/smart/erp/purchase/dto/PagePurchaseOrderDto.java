package org.smart.erp.purchase.dto;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;

@Data
public class PagePurchaseOrderDto {

    private Integer pageNum;

    private Integer pageSize;

    private String purchaseOrderNo;

    private Long materialId;

    private Long supplierId;

    private PurchaseOrderStatus status;
}

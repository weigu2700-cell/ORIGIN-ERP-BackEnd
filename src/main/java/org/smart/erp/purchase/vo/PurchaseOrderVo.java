package org.smart.erp.purchase.vo;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseOrderStatus;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderVo {

    private Long id;

    private String purchaseOrderNo;

    private Long purchaseDemandId;

    private String purchaseDemandNo;

    private Long materialId;

    private String materialName;

    private String materialCode;

    private Long supplierId;

    private String supplierName;

    private String supplierCode;

    private BigDecimal plannedQuantity;

    private BigDecimal completeQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    // 订单日期
    private LocalDateTime orderDate;

    // 预计交货日期
    private LocalDateTime expectedDeliveryDate;

    private LocalDateTime actualDeliveryDate;

    private PurchaseOrderStatus status;

}

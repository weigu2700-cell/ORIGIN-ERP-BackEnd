package org.smart.erp.purchase.vo;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseDemandSourceType;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseDemandVo {

    private Long id;

    private String purchaseDemandNo;

    private Long materialId;

    private BigDecimal purchaseQuantity;

    private PurchaseDemandSourceType sourceType;

    private String sourceNo;

    private PurchaseDemandStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

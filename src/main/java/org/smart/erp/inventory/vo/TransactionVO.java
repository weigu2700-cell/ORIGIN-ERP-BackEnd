package org.smart.erp.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionVO {

    private Long id;

    private Long warehouseId;

    private String warehouseName;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private Integer transactionType;

    private String transactionTypeName;

    private String businessType;

    private String businessNo;

    private BigDecimal quantity;

    private BigDecimal beforeOnHand;

    private BigDecimal afterOnHand;

    private BigDecimal beforeReserved;

    private BigDecimal afterReserved;

    private LocalDateTime createTime;

}
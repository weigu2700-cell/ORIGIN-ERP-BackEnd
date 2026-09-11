package org.smart.erp.purchase.vo;

import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseInStockStatus;
import org.smart.erp.purchase.enums.PurchaseInStockType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseInStockVo {

    private Long id;

    private String purchaseInStockNo;

    private Long purchaseOrderId;

    private String purchaseOrderNo;

    private Long supplierId;

    private String supplierName;

    private String supplierCode;

    private Long materialId;

    private String materialName;

    private String materialCode;

    private Long warehouseId;

    private String warehouseName;

    private String warehouseCode;

    // 库位
    private String storageLocation;

    /** 入库批次 */
    private String batchNo;

    private PurchaseInStockType inType;

    /** 入库单状态 */
    private PurchaseInStockStatus status;

    private String remark;

    private BigDecimal inQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private LocalDateTime productionDate;

    // 交货日期
    private LocalDateTime deliveryDate;

    // 过期日期
    private LocalDateTime expiryDate;

    /** 操作员 */
    private String operator;

    /** 实际入库时间 */
    private LocalDateTime inDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

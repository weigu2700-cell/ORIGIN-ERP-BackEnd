package org.smart.erp.purchase.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.smart.erp.purchase.enums.PurchaseInStockType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseInStockAddDto {

    @NotNull(message = "采购订单不能为空")
    private Long purchaseOrderId;

    /** 供应商可由采购订单带出，非必填 */
    private Long supplierId;

    @NotNull(message = "物料不能为空")
    private Long materialId;

    /** 仓库：采购订单表暂无仓库字段，审批自动生成时为空，待实际收货入库时补充 */
    private Long warehouseId;

    /** 库位，非必填 */
    private String storageLocation;

    @NotNull(message = "入库类型不能为空")
    private PurchaseInStockType inType;

    /** 备注，非必填 */
    private String remark;

    @NotNull(message = "入库数量不能为空")
    @Positive(message = "入库数量必须大于 0")
    private BigDecimal inQuantity;

    @NotNull(message = "单价不能为空")
    @PositiveOrZero(message = "单价不能为负数")
    private BigDecimal unitPrice;

    /** 总金额可由 数量 × 单价 计算得出，非必填 */
    private BigDecimal totalAmount;

    /** 生产日期，非必填 */
    private LocalDateTime productionDate;

    /** 送货日期，非必填 */
    private LocalDateTime deliveryDate;

    /** 过期日期，非必填 */
    private LocalDateTime expiryDate;

}

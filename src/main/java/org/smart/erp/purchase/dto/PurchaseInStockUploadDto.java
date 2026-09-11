package org.smart.erp.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购入库单上架请求参数。
 * <p>
 * 物料、入库数量、采购订单号等均可由入库单 id 查询得到，无需前端传入；
 * 这里只保留上架动作必须现场确认的信息：放到哪个仓库/库位、属于哪个批次。
 */
@Data
public class PurchaseInStockUploadDto {

    /** 仓库：入库单已指定仓库时可不传，未指定时必填 */
    private Long warehouseId;

    /** 库位：货品存放位置，必填 */
    @NotBlank(message = "库位不能为空")
    private String storageLocation;

    /** 入库批次 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 过期日期 */
    private LocalDateTime expiryDate;
}

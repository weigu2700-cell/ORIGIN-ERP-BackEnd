package org.smart.erp.inventory.dto;

import lombok.Data;

@Data
public class MaterialStockPageDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long materialId;

    private Long warehouseId;

    /** 物料编码精确匹配。 */
    private String materialCode;

    /** 物料编码或名称模糊匹配。 */
    private String keyword;
}

package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.BOMStatus;

@Data
public class pageBOMDto {

    private Integer pageNum;

    private Integer pageSize;

    private String bomNo;

    private Long materialId;

    private BOMStatus status;
}

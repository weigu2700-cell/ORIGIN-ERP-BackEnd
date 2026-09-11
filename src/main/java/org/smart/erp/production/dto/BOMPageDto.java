package org.smart.erp.production.dto;

import lombok.Data;
import org.smart.erp.production.enums.BOMStatus;

@Data
public class BOMPageDto {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String bomNo;

    private Long materialId;

    private BOMStatus status;
}

package org.smart.erp.production.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BOMAddDto {

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "明细不能为空")
    private List<BOMItemAddDto> bomItems;

}

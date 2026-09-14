package org.smart.erp.production.dto;

import lombok.Data;

import java.util.List;

@Data
public class BOMCacheDto {

    private Long bomId;

    private String bomNo;

    private Long materialId;

    private Integer version;

    private List<BOMItemCacheDto> items;
}

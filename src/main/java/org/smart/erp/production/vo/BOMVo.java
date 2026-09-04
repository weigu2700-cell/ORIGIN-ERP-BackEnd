package org.smart.erp.production.vo;

import lombok.Data;
import org.smart.erp.production.enums.BOMStatus;

import java.util.List;

@Data
public class BOMVo {

    private Long id;

    private String bomNo;

    private Long materialId;

    private String materialName;

    private String materialCode;

    private BOMStatus status;

    private List<BOMItemVo> bomItems;

}

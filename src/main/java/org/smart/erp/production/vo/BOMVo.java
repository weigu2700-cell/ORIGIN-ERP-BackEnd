package org.smart.erp.production.vo;

import lombok.Data;
import org.smart.erp.production.enums.BOMStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BOMVo {

    private Long id;

    private String bomNo;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BOMStatus status;

    private Integer version;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<BOMItemVo> bomItems;

}

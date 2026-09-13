package org.smart.erp.master.dto.WorkshopDto;

import lombok.Data;

@Data
public class WorkshopUpdateDto {

    private String name;

    private String shortName;

    private Long factoryId;

    private String remark;

}

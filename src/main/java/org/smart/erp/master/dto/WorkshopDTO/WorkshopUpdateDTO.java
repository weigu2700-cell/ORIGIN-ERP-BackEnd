package org.smart.erp.master.dto.WorkshopDTO;

import lombok.Data;

@Data
public class WorkshopUpdateDTO {

    private String name;

    private String shortName;

    private Long factoryId;

    private String remark;

}

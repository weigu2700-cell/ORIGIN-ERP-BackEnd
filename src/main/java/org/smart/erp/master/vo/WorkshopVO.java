package org.smart.erp.master.vo;

import lombok.Data;
import org.smart.erp.master.enums.WorkshopStatus;

@Data
public class WorkshopVO {

    private Long id;

    private String name;

    private String shortName;

    private Long factoryId;

    private String factoryName;

    private String remark;

    private WorkshopStatus status;
}

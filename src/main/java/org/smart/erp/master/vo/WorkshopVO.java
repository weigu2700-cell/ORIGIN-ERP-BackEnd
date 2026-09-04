package org.smart.erp.master.vo;

import lombok.Data;

@Data
public class WorkshopVO {

    private Long id;

    private String name;

    private String shortName;

    private Long factoryId;

    private String factoryName;

    private String remark;

    /** 1 启用，0 停用 */
    private Integer status;
}

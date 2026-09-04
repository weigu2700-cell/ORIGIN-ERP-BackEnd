package org.smart.erp.master.vo;

import lombok.Data;

@Data
public class FactoryVO {

    private String id;

    private String name;

    private String code;

    private String shortName;

    /** 1 启用，0 停用 */
    private Integer status;

    private String address;

    private String remark;


}

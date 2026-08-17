package org.smart.erp.master.vo;

import lombok.Data;
import org.smart.erp.master.enums.FactoryStatus;

@Data
public class FactoryVO {

    private String id;

    private String name;

    private String code;

    private String shortName;

    private FactoryStatus status;

    private String address;

    private String remark;


}

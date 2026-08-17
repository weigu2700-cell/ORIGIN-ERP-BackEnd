package org.smart.erp.master.enums;

import lombok.Getter;

@Getter
public enum WarehouseStatus {
    ENABLE(0, "启用"),
    DISABLE(1, "禁用");

    private final Integer code;
    private final String desc;

    WarehouseStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

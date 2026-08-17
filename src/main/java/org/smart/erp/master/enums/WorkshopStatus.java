package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum WorkshopStatus {
    ENABLE(0, "启用"),
    DISABLE(1, "禁用");

    @EnumValue
    private final Integer code;
    private final String desc;

    WorkshopStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProductionLineStatus {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");

    @EnumValue
    private final Integer code;
    private final String desc;

    ProductionLineStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package org.smart.erp.system.Enum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum RoleEnum {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");

    @EnumValue
    private final int code;
    private final String desc;

    RoleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

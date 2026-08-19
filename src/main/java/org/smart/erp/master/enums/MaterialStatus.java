package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum MaterialStatus {
    ENABLE(1, "启用"),
    DISABLE(2, "停用");

    @EnumValue
    private final int code;
    private final String desc;

    MaterialStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

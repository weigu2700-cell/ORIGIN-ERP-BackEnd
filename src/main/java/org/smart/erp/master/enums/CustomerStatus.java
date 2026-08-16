package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum CustomerStatus {
    ACTIVE(1, "有效"),
    INACTIVE(2, "无效");

    @EnumValue
    private final int code;
    private final String desc;

    CustomerStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SupplierStatus {
    ACTIVE(1, "有效"),
    INACTIVE(0, "无效");

    @EnumValue
    private final Integer code;
    private final String desc;

    SupplierStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

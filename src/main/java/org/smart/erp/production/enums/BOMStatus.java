package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BOMStatus {
    DRAFT(0, "草稿"),
    ACTIVE(1, "使用"),
    INACTIVE(2, "停用");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    BOMStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

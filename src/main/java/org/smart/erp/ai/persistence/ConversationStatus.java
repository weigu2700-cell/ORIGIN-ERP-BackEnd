package org.smart.erp.ai.persistence;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ConversationStatus {
    NORMAL(0,"正常"),
    ARCHIVED(1,"已归档");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    ConversationStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

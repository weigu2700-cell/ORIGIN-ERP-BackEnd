package org.smart.erp.ai.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiStreamType {
    COMPLETE(0,"完成"),
    ERROR(1,"失败"),
    TITLE(2,"生成标题");

    @EnumValue
    private final Integer code;
    @JsonValue
    private final String desc;

    AiStreamType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

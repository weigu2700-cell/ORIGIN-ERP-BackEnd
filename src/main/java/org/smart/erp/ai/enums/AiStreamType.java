package org.smart.erp.ai.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiStreamType {
    CONTENT,
    ERROR,
    TITLE,
    ACTION

}

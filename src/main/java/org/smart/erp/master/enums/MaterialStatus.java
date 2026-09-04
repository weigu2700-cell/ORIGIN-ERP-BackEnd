package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Map;

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

    /**
     * 宽容反序列化：兼容 "ENABLE"/"DISABLE"、1/2、{"code":1} 等前端各种传法
     */
    @JsonCreator
    public static MaterialStatus from(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Object code = map.get("code");
            if (code == null) {
                code = map.get("name");
            }
            if (code == null) {
                code = map.get("value");
            }
            if (code == null) {
                code = map.get("desc");
            }
            value = code;
        }
        if (value instanceof Number n) {
            for (MaterialStatus s : values()) {
                if (s.code == n.intValue()) {
                    return s;
                }
            }
            throw new IllegalArgumentException("未知状态: " + n);
        }
        String str = String.valueOf(value).trim();
        for (MaterialStatus s : values()) {
            if (s.name().equalsIgnoreCase(str)
                    || (String.valueOf(s.code).equals(str))
                    || s.desc.equals(str)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知状态: " + str);
    }
}

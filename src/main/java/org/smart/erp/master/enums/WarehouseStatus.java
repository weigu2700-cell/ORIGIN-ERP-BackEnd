package org.smart.erp.master.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Map;

@Getter
public enum WarehouseStatus {
    ENABLE(0, "启用"),
    DISABLE(1, "禁用");

    private final Integer code;
    private final String desc;

    WarehouseStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 宽容反序列化：兼容 "DISABLE"、0/1、{"code":1}、{"name":"DISABLE"} 等前端各种传法
     */
    @JsonCreator
    public static WarehouseStatus from(Object value) {
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
            for (WarehouseStatus s : values()) {
                if (s.code != null && s.code.intValue() == n.intValue()) {
                    return s;
                }
            }
            throw new IllegalArgumentException("未知状态: " + n);
        }
        String str = String.valueOf(value).trim();
        for (WarehouseStatus s : values()) {
            if (s.name().equalsIgnoreCase(str)
                    || (s.code != null && String.valueOf(s.code).equals(str))
                    || s.desc.equals(str)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知状态: " + str);
    }
}

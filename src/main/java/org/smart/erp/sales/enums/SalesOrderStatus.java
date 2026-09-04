package org.smart.erp.sales.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Map;

@Getter
public enum SalesOrderStatus {
    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    @EnumValue
    private final Integer code;
    private final String desc;

    private SalesOrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 宽容反序列化：兼容 "CANCELLED"、0/3、{"code":3} 等前端各种传法
     */
    @JsonCreator
    public static SalesOrderStatus from(Object value) {
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
            for (SalesOrderStatus s : values()) {
                if (s.code != null && s.code.intValue() == n.intValue()) {
                    return s;
                }
            }
            throw new IllegalArgumentException("未知状态: " + n);
        }
        String str = String.valueOf(value).trim();
        for (SalesOrderStatus s : values()) {
            if (s.name().equalsIgnoreCase(str)
                    || (s.code != null && String.valueOf(s.code).equals(str))
                    || s.desc.equals(str)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知状态: " + str);
    }
}

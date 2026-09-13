package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProductionOrderStatus {
    DRAFT(0, "草稿"),
    RELEASED(1, "已下达"),
    IN_PROGRESS(2, "生产中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    @EnumValue
    @com.fasterxml.jackson.annotation.JsonValue
    private final int code;
    private final String desc;

    ProductionOrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 查询参数绑定用：兼容数字码（如 "2"）与枚举名（如 "IN_PROGRESS"）。
     */
    public static ProductionOrderStatus from(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String trimmed = source.trim();
        try {
            int code = Integer.parseInt(trimmed);
            for (ProductionOrderStatus value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        } catch (NumberFormatException ignored) {
            // 非数字，按名称匹配
        }
        try {
            return ProductionOrderStatus.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

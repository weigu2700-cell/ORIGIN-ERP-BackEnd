package org.smart.erp.purchase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PurchaseInStockStatus {
    DRAFT(0, "草稿"),
    APPROVED(1, "已审核"),
    UPLOADED(2, "已上架");

    @EnumValue
    @com.fasterxml.jackson.annotation.JsonValue
    private final int code;
    private final String desc;

    PurchaseInStockStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 查询参数绑定用：兼容数字码（如 "1"）与枚举名（如 "APPROVED"）。
     */
    public static PurchaseInStockStatus from(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String trimmed = source.trim();
        try {
            int code = Integer.parseInt(trimmed);
            for (PurchaseInStockStatus value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        } catch (NumberFormatException ignored) {
            // 非数字，按名称匹配
        }
        try {
            return PurchaseInStockStatus.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductionReportStatus {
    DRAFT(0, "草稿"),
    APPROVED(1, "已审批"),
    CANCEL(2, "已取消"),
    REJECT(3, "已驳回"),
    FINISHED(4, "已完成");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    ProductionReportStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 查询参数绑定用：兼容数字码（如 "1"）与枚举名（如 "APPROVED"）。
     */
    public static ProductionReportStatus from(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String trimmed = source.trim();
        try {
            int code = Integer.parseInt(trimmed);
            for (ProductionReportStatus value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        } catch (NumberFormatException ignored) {
            // 非数字，按名称匹配
        }
        try {
            return ProductionReportStatus.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

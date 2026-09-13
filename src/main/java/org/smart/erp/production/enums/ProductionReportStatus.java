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
    private final Integer code;
    @JsonValue
    private final String desc;

    ProductionReportStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

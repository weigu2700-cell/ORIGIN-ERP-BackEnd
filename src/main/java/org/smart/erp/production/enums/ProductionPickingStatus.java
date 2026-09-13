package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductionPickingStatus {
    DRAFT(0, "草稿"),
    APPROVED(1, "已审批"),
    PICKED(2, "已领料"),
    CANCELLED(3, "已取消");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    ProductionPickingStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

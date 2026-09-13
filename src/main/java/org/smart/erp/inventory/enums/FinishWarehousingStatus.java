package org.smart.erp.inventory.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FinishWarehousingStatus {
    DRAFT(0, "草稿"),
    APPROVED(1, "已审批"),
    WAREHOUSED(2, "已入库"),
    CANCEL(3, "已取消");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    FinishWarehousingStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

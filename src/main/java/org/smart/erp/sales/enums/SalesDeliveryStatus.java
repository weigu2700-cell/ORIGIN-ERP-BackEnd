package org.smart.erp.sales.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SalesDeliveryStatus {
    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    CANCELLED(2, "已取消");

    @EnumValue
    private final int code;
    private final String desc;

    SalesDeliveryStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

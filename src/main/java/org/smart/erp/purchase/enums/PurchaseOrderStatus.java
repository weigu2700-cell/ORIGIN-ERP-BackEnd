package org.smart.erp.purchase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PurchaseOrderStatus {
    DRAFT(0, "草稿"),
    APPROVED(1, "已审批"),
    SHIPPED(2, "已发货"),
    RECEIVED(3, "已收货"),
    CLOSED(4, "已关闭");

    @EnumValue
    @com.fasterxml.jackson.annotation.JsonValue
    private final int code;
    private final String desc;

    PurchaseOrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package org.smart.erp.purchase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PurchaseDemandSourceType {
    PRODUCTION_ORDER(0, "生产订单"),
    OTHER(1, "其他");

    @EnumValue
    @com.fasterxml.jackson.annotation.JsonValue
    private final int code;
    private final String desc;

    PurchaseDemandSourceType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

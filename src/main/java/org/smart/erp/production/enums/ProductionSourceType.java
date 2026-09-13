package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProductionSourceType {
    SALES_ORDER(0,"销售订单");

    @EnumValue
    @com.fasterxml.jackson.annotation.JsonValue
    private final Integer code;
    private final String desc;

    ProductionSourceType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

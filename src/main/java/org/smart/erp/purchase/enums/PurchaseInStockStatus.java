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
}

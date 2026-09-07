package org.smart.erp.purchase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PurchaseInStockType {
    PURCHASE_NORMAL(0, "采购入库"),
    PURCHASE_RETURN(1, "采购退货"),
    PURCHASE_GIFT(2, "赠品入库");

    @EnumValue
    private final int code;
    private final String desc;

    PurchaseInStockType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

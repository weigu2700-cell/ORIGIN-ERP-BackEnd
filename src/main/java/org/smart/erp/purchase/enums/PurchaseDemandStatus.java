package org.smart.erp.purchase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PurchaseDemandStatus {
    DRAFT(0, "草稿"),
    APPROVED(1, "已审批"),
    CLOSED(2, "已关闭");

    @EnumValue
    private final int code;
    private final String desc;

    PurchaseDemandStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package org.smart.erp.inventory.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TransactionType {
    INBOUND(1, "入库"),
    RESERVE(2, "预占"),
    RELEASE(3, "释放预占"),
    OUTBOUND(4, "出库");

    @EnumValue
    private final Integer code;
    private final String desc;

    TransactionType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

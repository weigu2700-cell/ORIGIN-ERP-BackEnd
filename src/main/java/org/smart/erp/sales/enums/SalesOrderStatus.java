package org.smart.erp.sales.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SalesOrderStatus {
    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    @EnumValue
    private final Integer code;
    private final String desc;

    private SalesOrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

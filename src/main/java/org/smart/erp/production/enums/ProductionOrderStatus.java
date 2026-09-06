package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProductionOrderStatus {
    DRAFT(0, "草稿"),
    RELEASED(1, "已下达"),
    IN_PROGRESS(2, "生产中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    @EnumValue
    private final int code;
    private final String desc;

    ProductionOrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

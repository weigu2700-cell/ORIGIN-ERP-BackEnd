package org.smart.erp.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProductionStatus {
    PENDING(0,"待生产"),
    PLANNED(1,"已计划"),
    CANCELLED(2,"已取消");

    @EnumValue
    private final Integer code;
    private final String desc;

    ProductionStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

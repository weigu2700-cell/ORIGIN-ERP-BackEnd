package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum WarehouseType {
    FINISHED(0, "成品仓"),
    MATERIAL(1, "原材料仓"),
    SEMI_FINISHED(2, "半成品仓"),
    SCRAP(3, "废品仓"),
    OTHER(4, "其他仓");

    @EnumValue
    private final Integer code;
    private final String desc;

    WarehouseType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

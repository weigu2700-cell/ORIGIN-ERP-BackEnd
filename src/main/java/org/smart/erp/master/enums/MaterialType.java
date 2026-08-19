package org.smart.erp.master.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum MaterialType {
    RAW_MATERIAL(1, "原材料"),
    SEMI_FINISHED(2, "半成品"),
    PACKAGING(3, "包装材料"),
    CONSUMABLE(4, "辅料"),
    OTHER(5, "其他物料");

    @EnumValue
    private final int code;
    private final String desc;

    MaterialType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

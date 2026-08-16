package org.smart.erp.system.Enum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PermissionType {
    MENU(1,"菜单"),
    BUTTON(2,"按钮");

    @EnumValue
    private final int code;
    private final String desc;

    PermissionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

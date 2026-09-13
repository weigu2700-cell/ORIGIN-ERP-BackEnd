package org.smart.erp.system.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.system.Enum.RoleEnum;
import org.springframework.stereotype.Component;

@Component
public class StringToRoleEnumConverter extends AbstractCodeEnumConverter<RoleEnum> {
    public StringToRoleEnumConverter() {
        super(RoleEnum.class);
    }
}

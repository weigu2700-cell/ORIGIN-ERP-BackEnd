package org.smart.erp.system.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.system.Enum.PermissionType;
import org.springframework.stereotype.Component;

@Component
public class StringToPermissionTypeConverter extends AbstractCodeEnumConverter<PermissionType> {
    public StringToPermissionTypeConverter() {
        super(PermissionType.class);
    }
}

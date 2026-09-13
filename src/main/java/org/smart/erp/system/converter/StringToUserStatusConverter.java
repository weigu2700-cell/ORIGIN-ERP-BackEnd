package org.smart.erp.system.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.system.Enum.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToUserStatusConverter extends AbstractCodeEnumConverter<UserStatus> {
    public StringToUserStatusConverter() {
        super(UserStatus.class);
    }
}

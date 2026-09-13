package org.smart.erp.system.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.system.Enum.DeptStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToDeptStatusConverter extends AbstractCodeEnumConverter<DeptStatus> {
    public StringToDeptStatusConverter() {
        super(DeptStatus.class);
    }
}

package org.smart.erp.system.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.system.Enum.Status;
import org.springframework.stereotype.Component;

@Component
public class StringToStatusConverter extends AbstractCodeEnumConverter<Status> {
    public StringToStatusConverter() {
        super(Status.class);
    }
}

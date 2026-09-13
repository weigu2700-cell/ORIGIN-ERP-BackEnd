package org.smart.erp.master.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.master.enums.CustomerStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToCustomerStatusConverter extends AbstractCodeEnumConverter<CustomerStatus> {
    public StringToCustomerStatusConverter() {
        super(CustomerStatus.class);
    }
}

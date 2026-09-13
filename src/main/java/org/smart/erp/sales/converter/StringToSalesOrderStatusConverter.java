package org.smart.erp.sales.converter;

import org.smart.erp.common.converter.AbstractCodeEnumConverter;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.springframework.stereotype.Component;

@Component
public class StringToSalesOrderStatusConverter extends AbstractCodeEnumConverter<SalesOrderStatus> {
    public StringToSalesOrderStatusConverter() {
        super(SalesOrderStatus.class);
    }
}

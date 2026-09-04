package org.smart.erp.sales.converter;

import org.smart.erp.sales.enums.SalesOrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 查询参数绑定用：将列表接口传入的 status 字符串（数字码或枚举名）转换为 SalesOrderStatus 枚举。
 * GET 列表接口以 @ModelAttribute 形式绑定，走 Spring ConversionService，而非 Jackson，故需此转换器。
 */
@Component
public class StringToSalesOrderStatusConverter implements Converter<String, SalesOrderStatus> {

    @Override
    public SalesOrderStatus convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SalesOrderStatus.from(source.trim());
    }
}

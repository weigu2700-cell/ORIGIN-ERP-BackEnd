package org.smart.erp.common.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DateCodeRuleUtil {

    public String setDateCodeRule(String fond , Long id ) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String idStr = id.toString().substring(0, 3);
        return fond + date + idStr;
    }
}

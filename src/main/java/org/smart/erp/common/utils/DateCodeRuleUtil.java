package org.smart.erp.common.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateCodeRuleUtil {

    /**
     * 生成业务编码：前缀 + 日期(8位) + 时间(毫秒级9位) + id后4位
     * 保证同一前缀、同一 id 下每次调用编码唯一，避免撞唯一索引。
     */
    public String setDateCodeRule(String fond, Long id) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String idStr = id == null ? "" : id.toString();
        if (idStr.length() > 4) {
            idStr = idStr.substring(idStr.length() - 4);
        }
        return fond + timestamp + idStr;
    }
}

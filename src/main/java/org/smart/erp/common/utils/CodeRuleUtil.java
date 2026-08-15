package org.smart.erp.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.service.DeptService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class CodeRuleUtil {

    private final DeptService deptService;

    public CodeRuleUtil(DeptService deptService) {
        this.deptService = deptService;
    }

    public String createCodeRule(String codeRulePrefix) {
        String nowTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long maxSeq = deptService.list(new LambdaQueryWrapper<Dept>()
                        .likeRight(Dept::getCode, codeRulePrefix + nowTime))
                .stream()
                .map(d -> parseSeq(d.getCode(), codeRulePrefix + nowTime))
                .max(Long::compare)
                .orElse(0L);
        return codeRulePrefix + nowTime + (maxSeq + 1);
    }

    private long parseSeq(String code, String prefixWithDate) {
        if (code == null || !code.startsWith(prefixWithDate)) return 0L;
        try {
            return Long.parseLong(code.substring(prefixWithDate.length()));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}


package org.smart.erp.eip.service.impl;

import org.junit.jupiter.api.Test;
import org.smart.erp.common.exception.BusinessException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemNotificationServiceImplTests {
    @Test
    void rendersOnlyNamedVariables() {
        assertThat(SystemNotificationServiceImpl.render("你好，{{name}}", Map.of("name", "顾威")))
                .isEqualTo("你好，顾威");
    }

    @Test
    void missingVariableFailsInsteadOfPublishingPartialMessage() {
        assertThatThrownBy(() -> SystemNotificationServiceImpl.render("订单 {{orderNo}} 已完成", Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("orderNo");
    }

    @Test
    void rejectsUnsupportedPlaceholderSyntax() {
        assertThatThrownBy(() -> SystemNotificationServiceImpl.render("订单 {{order-no}}", Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }
}

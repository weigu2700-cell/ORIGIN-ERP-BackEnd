package org.smart.erp.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.smart.erp.ai.enums.AiActionStatus;
import org.smart.erp.ai.enums.AiActionType;
import org.smart.erp.common.config.RedisConfig;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AiPendingActionSerializationTest {
    @Test
    @SuppressWarnings("unchecked")
    void redisRoundTripRetainsRecordTypeAndBusinessFields() {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var template = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class), mapper);
        var serializer = (RedisSerializer<Object>) template.getValueSerializer();
        var now = LocalDateTime.of(2026, 11, 2, 10, 0);
        var action = new AiPendingAction("token", 7L, 42L, AiActionType.APPROVE_PURCHASE_ORDER,
                100L, "PUR-100", 3, "审批采购订单", "确认后执行", AiActionStatus.PENDING, now, now.plusMinutes(10));

        assertEquals(action, serializer.deserialize(serializer.serialize(action)));
    }
}

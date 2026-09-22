package org.smart.erp.ai.conversation.service.impl;

import org.junit.jupiter.api.Test;
import org.smart.erp.ai.conversation.entity.AiConversation;
import org.smart.erp.ai.conversation.mapper.AiConversationMapper;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiConversationServiceImplTest {

    @Test
    void returnsOnlyTheCurrentUsersConversation() {
        AtomicReference<Long> userId = new AtomicReference<>(7L);
        CurrentUser currentUser = new CurrentUser() {
            @Override
            public Long getUserId() {
                return userId.get();
            }

            @Override
            public String getUsername() {
                return "tester";
            }
        };
        AiConversation conversation = new AiConversation();
        conversation.setId(42L);
        conversation.setUserId(7L);
        AiConversationMapper mapper = (AiConversationMapper) Proxy.newProxyInstance(
                AiConversationMapper.class.getClassLoader(),
                new Class<?>[]{AiConversationMapper.class},
                (proxy, method, args) -> method.getName().equals("selectById")
                        && Long.valueOf(42L).equals(args[0]) ? conversation : null);

        AiConversationServiceImpl service = new AiConversationServiceImpl(currentUser, mapper);
        assertSame(conversation, service.getOwnedConversation(42L));

        userId.set(8L);
        assertThrows(BusinessException.class, () -> service.getOwnedConversation(42L));
        assertThrows(BusinessException.class, () -> service.getOwnedConversation(99L));
    }
}

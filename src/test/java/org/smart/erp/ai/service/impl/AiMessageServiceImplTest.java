package org.smart.erp.ai.service.impl;

import org.junit.jupiter.api.Test;
import org.smart.erp.ai.persistence.AiMessageMapper;
import org.smart.erp.ai.persistence.ConversationStatus;
import org.smart.erp.ai.persistence.impl.AiConversation;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiMessageServiceImplTest {
    @Test
    void archivedConversationCannotAppendMessages() {
        var conversations = mock(AiConversationService.class);
        var mapper = mock(AiMessageMapper.class);
        var conversation = new AiConversation();
        conversation.setStatus(ConversationStatus.ARCHIVED);
        when(conversations.getOwnedConversation(42L)).thenReturn(conversation);
        var service = new AiMessageServiceImpl(conversations, mapper);

        var error = assertThrows(BusinessException.class, () -> service.addMessage(42L, "user", "继续查询"));
        assertEquals("已归档会话不可发送消息", error.getMessage());
        verifyNoInteractions(mapper);
    }

    @Test
    void ownershipFailureCannotAppendMessages() {
        var conversations = mock(AiConversationService.class);
        var mapper = mock(AiMessageMapper.class);
        when(conversations.getOwnedConversation(42L)).thenThrow(new BusinessException(403, "无权访问"));
        var service = new AiMessageServiceImpl(conversations, mapper);

        assertThrows(BusinessException.class, () -> service.addMessage(42L, "user", "继续查询"));
        verifyNoInteractions(mapper);
    }

    @Test
    void activeConversationCanAppendMessages() {
        var conversations = mock(AiConversationService.class);
        var mapper = mock(AiMessageMapper.class);
        var conversation = new AiConversation();
        conversation.setStatus(ConversationStatus.NORMAL);
        when(conversations.getOwnedConversation(42L)).thenReturn(conversation);
        var service = new AiMessageServiceImpl(conversations, mapper);

        service.addMessage(42L, "user", "库存查询");
        verify(mapper).insert(argThat((org.smart.erp.ai.persistence.impl.AiMessage message) ->
                message.getConversationId().equals(42L) && message.getContent().equals("库存查询")));
    }
}

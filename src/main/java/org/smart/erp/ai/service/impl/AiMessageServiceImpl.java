package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.ai.entity.AiMessage;
import org.smart.erp.ai.mapper.AiMessageMapper;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.ai.service.AiMessageService;
import org.springframework.stereotype.Service;

@Service
public class AiMessageServiceImpl
    extends ServiceImpl<AiMessageMapper, AiMessage>
        implements AiMessageService
{

    private final AiConversationService aiConversationService;

    public AiMessageServiceImpl(AiConversationService aiConversationService) {
        this.aiConversationService = aiConversationService;
    }

    @Override
    public void addMessage(Long conversationId,String role, String message) {
        aiConversationService.getOwnedConversation(conversationId);
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole(role);
        aiMessage.setContent(message);
        save(aiMessage);
    }
}
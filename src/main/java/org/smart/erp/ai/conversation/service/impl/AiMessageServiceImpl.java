package org.smart.erp.ai.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.ai.conversation.entity.AiMessage;
import org.smart.erp.ai.conversation.mapper.AiMessageMapper;
import org.smart.erp.ai.conversation.service.AiConversationService;
import org.smart.erp.ai.conversation.service.AiMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

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
        saveMessage(conversationId,role,message);
    }

    @Override
    public void saveMessage(Long conversationId,String role, String message) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole(role);
        aiMessage.setContent(message);
        save(aiMessage);
    }

    @Override
    public List<AiMessage> listMessage(Long conversationId) {
        Long checkedConversationId =  aiConversationService.getOwnedConversation(conversationId).getId();
        return this.list(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, checkedConversationId)
                        .orderByAsc(AiMessage::getCreateTime)
                        .orderByAsc(AiMessage::getId)
        );
    }
}
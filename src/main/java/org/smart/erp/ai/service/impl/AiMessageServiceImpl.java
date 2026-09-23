package org.smart.erp.ai.service.impl;

import org.smart.erp.ai.service.AiMessageService;
import org.smart.erp.ai.service.AiConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.ai.persistence.AiMessage;
import org.smart.erp.ai.persistence.AiMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiMessageServiceImpl implements AiMessageService {

    private final AiConversationService aiConversationService;
    private final AiMessageMapper aiMessageMapper;

    public AiMessageServiceImpl(AiConversationService aiConversationService,
                                AiMessageMapper aiMessageMapper) {
        this.aiConversationService = aiConversationService;
        this.aiMessageMapper = aiMessageMapper;
    }

    @Override
    public void addMessage(Long conversationId, String role, String message) {
        aiConversationService.getOwnedConversation(conversationId);
        saveMessage(conversationId, role, message);
    }

    @Override
    public void saveMessage(Long conversationId, String role, String message) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole(role);
        aiMessage.setContent(message);
        aiMessageMapper.insert(aiMessage);
    }

    @Override
    public List<AiMessage> listMessage(Long conversationId) {
        Long checkedConversationId = aiConversationService.getOwnedConversation(conversationId).getId();
        return aiMessageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, checkedConversationId)
                .orderByAsc(AiMessage::getCreateTime)
                .orderByAsc(AiMessage::getId));
    }
}

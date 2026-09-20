package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.ai.entity.AiMessage;
import org.smart.erp.ai.mapper.AiConversationMapper;
import org.smart.erp.ai.mapper.AiMessageMapper;
import org.smart.erp.ai.service.AiMessageService;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AiMessageServiceImpl
    extends ServiceImpl<AiMessageMapper, AiMessage>
        implements AiMessageService
{

    private final CurrentUser currentUser;
    private final AiConversationMapper aiConversationMapper;

    public AiMessageServiceImpl(
            CurrentUser currentUser,
            AiConversationMapper aiConversationMapper
    ) {
        this.currentUser = currentUser;
        this.aiConversationMapper = aiConversationMapper;
    }

    @Override
    public void addMessage(Long conversationId,String role, String message) {
        Long userId = currentUser.getUserId();
        Long conversationUserId = aiConversationMapper.selectById(conversationId).getUserId();

        if (!userId.equals(conversationUserId)) {
            throw new BusinessException(403, "当前对话与用户不匹配");
        }
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole(role);
        aiMessage.setContent(message);
        save(aiMessage);

    }
}
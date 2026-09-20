package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.ai.entity.AiConversation;
import org.smart.erp.ai.enums.ConversationStatus;
import org.smart.erp.ai.mapper.AiConversationMapper;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AiConversationServiceImpl
    extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService
{
    private final CurrentUser currentUser;

    public AiConversationServiceImpl(
            CurrentUser currentUser
    ) {
        this.currentUser = currentUser;
    }

    @Override
    public void addConversation() {
        AiConversation aiConversation = new AiConversation();
        aiConversation.setUserId(currentUser.getUserId());
        aiConversation.setTitle("新对话");
        aiConversation.setStatus(ConversationStatus.NORMAL);
        save(aiConversation);
    }
}

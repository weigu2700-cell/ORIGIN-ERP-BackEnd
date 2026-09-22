package org.smart.erp.ai.conversation.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.ai.conversation.dto.ConversationResult;
import org.smart.erp.ai.conversation.entity.AiConversation;

import java.util.List;

public interface AiConversationService extends IService<AiConversation> {
    ConversationResult addConversation();

    AiConversation getOwnedConversation(Long conversationId);

    List<AiConversation> listConversation();

    ConversationResult archiveConversation(Long conversationId);

    void writeTitle(Long conversationId, String title);
}

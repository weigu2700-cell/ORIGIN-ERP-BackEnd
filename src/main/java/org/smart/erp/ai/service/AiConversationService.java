package org.smart.erp.ai.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.ai.dto.request.ConversationResult;
import org.smart.erp.ai.entity.AiConversation;

import java.util.List;

public interface AiConversationService extends IService<AiConversation> {
    ConversationResult addConversation();

    AiConversation getOwnedConversation(Long conversationId);

    List<AiConversation> listConversation();

    ConversationResult archiveConversation(Long conversationId);
}

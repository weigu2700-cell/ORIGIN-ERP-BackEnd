package org.smart.erp.ai.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.ai.entity.AiConversation;

public interface AiConversationService extends IService<AiConversation> {
    void addConversation();

    AiConversation getOwnedConversation(Long conversationId);
}

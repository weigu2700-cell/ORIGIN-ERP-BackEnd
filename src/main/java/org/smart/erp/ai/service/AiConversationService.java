package org.smart.erp.ai.service;

import org.smart.erp.ai.result.ConversationResult;
import org.smart.erp.ai.persistence.AiConversation;

import java.util.List;

/** 对话用例入口；不向 Controller 暴露 MyBatis 的通用写接口。 */
public interface AiConversationService {
    ConversationResult addConversation();

    AiConversation getOwnedConversation(Long conversationId);

    List<AiConversation> listConversation();

    ConversationResult archiveConversation(Long conversationId);

    void writeTitle(Long conversationId, String title);
}

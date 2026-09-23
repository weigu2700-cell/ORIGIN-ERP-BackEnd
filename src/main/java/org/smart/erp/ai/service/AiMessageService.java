package org.smart.erp.ai.service;

import org.smart.erp.ai.persistence.AiMessage;

import java.util.List;

public interface AiMessageService {
    void addMessage(Long conversationId, String role, String message);

    /** 仅在调用方已完成对话归属校验后使用，供异步流保存助手回复。 */
    void saveMessage(Long conversationId, String role, String message);

    List<AiMessage> listMessage(Long conversationId);
}

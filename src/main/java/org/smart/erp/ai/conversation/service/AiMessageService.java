package org.smart.erp.ai.conversation.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.ai.conversation.entity.AiMessage;

import java.util.List;

public interface AiMessageService extends IService<AiMessage> {

    void addMessage(Long conversationId,String role, String message);

    void saveMessage(Long conversationId,String role,String message);

    List<AiMessage> listMessage(Long conversationId);
}

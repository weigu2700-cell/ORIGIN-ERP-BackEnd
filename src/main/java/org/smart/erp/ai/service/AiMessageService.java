package org.smart.erp.ai.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.ai.entity.AiMessage;

public interface AiMessageService extends IService<AiMessage> {

    void addMessage(Long conversationId,String role, String message);
}

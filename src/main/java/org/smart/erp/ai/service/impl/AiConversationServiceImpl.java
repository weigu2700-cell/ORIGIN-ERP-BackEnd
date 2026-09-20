package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.ai.entity.AiConversation;
import org.smart.erp.ai.mapper.AiConversationMapper;
import org.smart.erp.ai.service.AiAssistantService;
import org.smart.erp.ai.service.AiConversationService;
import org.springframework.stereotype.Service;

@Service
public class AiConversationServiceImpl
    extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService
{
}

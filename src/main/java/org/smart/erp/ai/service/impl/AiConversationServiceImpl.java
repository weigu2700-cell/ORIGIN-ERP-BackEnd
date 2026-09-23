package org.smart.erp.ai.service.impl;

import org.smart.erp.ai.service.AiConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.ai.result.ConversationResult;
import org.smart.erp.ai.persistence.AiConversation;
import org.smart.erp.ai.persistence.ConversationStatus;
import org.smart.erp.ai.persistence.AiConversationMapper;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiConversationServiceImpl implements AiConversationService {

    private final CurrentUser currentUser;
    private final AiConversationMapper aiConversationMapper;

    public AiConversationServiceImpl(CurrentUser currentUser,
                                     AiConversationMapper aiConversationMapper) {
        this.currentUser = currentUser;
        this.aiConversationMapper = aiConversationMapper;
    }

    @Override
    public ConversationResult addConversation() {
        AiConversation aiConversation = new AiConversation();
        aiConversation.setUserId(currentUser.getUserId());
        aiConversation.setTitle("新对话");
        aiConversation.setStatus(ConversationStatus.NORMAL);
        aiConversationMapper.insert(aiConversation);
        return new ConversationResult(aiConversation.getId());
    }

    @Override
    public AiConversation getOwnedConversation(Long conversationId) {
        if (conversationId == null) {
            throw new BusinessException(404, "对话不存在");
        }
        AiConversation conversation = aiConversationMapper.selectById(conversationId);
        if (conversation == null || Integer.valueOf(1).equals(conversation.getDeleted())) {
            throw new BusinessException(404, "对话不存在");
        }
        if (!currentUser.getUserId().equals(conversation.getUserId())) {
            throw new BusinessException(403, "当前对话与用户不匹配");
        }
        return conversation;
    }

    @Override
    public List<AiConversation> listConversation() {
        return aiConversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, currentUser.getUserId())
                .eq(AiConversation::getDeleted, 0)
                .orderByDesc(AiConversation::getUpdateTime));
    }

    @Override
    public ConversationResult archiveConversation(Long conversationId) {
        AiConversation conversation = getOwnedConversation(conversationId);
        conversation.setStatus(ConversationStatus.ARCHIVED);
        aiConversationMapper.updateById(conversation);
        return new ConversationResult(conversation.getId());
    }

    @Override
    public void writeTitle(Long conversationId, String title) {
        AiConversation conversation = getOwnedConversation(conversationId);
        conversation.setTitle(title);
        aiConversationMapper.updateById(conversation);
    }
}

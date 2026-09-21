package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.smart.erp.ai.dto.request.ConversationResult;
import org.smart.erp.ai.entity.AiConversation;
import org.smart.erp.ai.enums.ConversationStatus;
import org.smart.erp.ai.mapper.AiConversationMapper;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AiConversationServiceImpl
    extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService
{

    private final CurrentUser currentUser;
    private final AiConversationMapper aiConversationMapper;

    public AiConversationServiceImpl(
            CurrentUser currentUser,
            AiConversationMapper aiConversationMapper
    ) {
        this.currentUser = currentUser;
        this.aiConversationMapper = aiConversationMapper;
    }

    @Override
    public AiConversation getOwnedConversation(Long conversationId) {
        Long userId = currentUser.getUserId();
        AiConversation conversation = Optional.ofNullable(
                aiConversationMapper.selectById(conversationId)
        ).orElseThrow(() -> new BusinessException(404, "对话不存在"));

        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(403, "当前对话与用户不匹配");
        }
        return conversation;
    }

    @Override
    public List<AiConversation> listConversation() {
        return aiConversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, currentUser.getUserId())
                        .eq(AiConversation::getDeleted, 0)
                        .orderByDesc(AiConversation::getUpdateTime))
                .stream()
                .toList();
    }

    @Override
    public ConversationResult archiveConversation(Long conversationId) {
        AiConversation conversation = getOwnedConversation(conversationId);
        conversation.setStatus(ConversationStatus.ARCHIVED);
        updateById(conversation);
        return new ConversationResult(conversation.getId());
    }

    @Override
    public ConversationResult addConversation() {
        AiConversation aiConversation = new AiConversation();
        aiConversation.setUserId(currentUser.getUserId());
        aiConversation.setTitle("新对话");
        aiConversation.setStatus(ConversationStatus.NORMAL);
        save(aiConversation);

        return new ConversationResult(aiConversation.getId());
    }

}

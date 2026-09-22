package org.smart.erp.ai.action.model;

import org.smart.erp.ai.action.model.AiActionStatus;
import org.smart.erp.ai.action.model.AiActionType;

import java.time.LocalDateTime;

public record AiPendingAction(
        String token,
        Long userId,
        Long conversationId,
        AiActionType actionType,
        Long bizId,
        String bizNo,
        Integer version,
        String title,
        String description,
        AiActionStatus status,
        LocalDateTime createTime,
        LocalDateTime expireTime
) {
}

package org.smart.erp.ai.tool.action.model;

import org.smart.erp.ai.tool.action.enums.AiActionStatus;
import org.smart.erp.ai.tool.action.enums.AiActionType;

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

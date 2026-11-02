package org.smart.erp.ai.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.smart.erp.ai.enums.AiActionStatus;
import org.smart.erp.ai.enums.AiActionType;

import java.time.LocalDateTime;

// Redis stores values as Object, so final record types need explicit type metadata.
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
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

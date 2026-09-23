package org.smart.erp.ai.result;

import org.smart.erp.ai.enums.AiActionType;

import java.time.LocalDateTime;

public record AiPendingActionResult(
        String token,
        AiActionType actionType,
        String bizNo,
        String title,
        String description,
        LocalDateTime expireTime
) {
}

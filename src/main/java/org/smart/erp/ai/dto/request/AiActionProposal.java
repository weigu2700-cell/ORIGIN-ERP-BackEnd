package org.smart.erp.ai.dto.request;

import org.smart.erp.ai.enums.AiActionType;

public record AiActionProposal(
        AiActionType type,
        Long bizId,
        String bizNo,
        Integer version,
        String title,
        String description
) {
}

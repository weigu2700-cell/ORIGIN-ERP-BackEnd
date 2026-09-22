package org.smart.erp.ai.action.model;

import org.smart.erp.ai.enums.AiActionType;

public record AiActionProposal(
        AiActionType actionType,
        Long bizId,
        String bizNo,
        Integer version,
        String title,
        String description
) {
}

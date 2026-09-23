package org.smart.erp.ai.tool.action.model;

import org.smart.erp.ai.tool.action.enums.AiActionType;

public record AiActionProposal(
        AiActionType actionType,
        Long bizId,
        String bizNo,
        Integer version,
        String title,
        String description
) {
}

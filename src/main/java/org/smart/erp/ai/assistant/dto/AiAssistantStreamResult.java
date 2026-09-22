package org.smart.erp.ai.assistant.dto;

import org.smart.erp.ai.assistant.model.AiStreamType;

public record AiAssistantStreamResult(
        AiStreamType type,
        String content
) {
}

package org.smart.erp.ai.assistant.dto;

public record AiAssistantRequest(
        Long conversationId,
        String message
) {
}

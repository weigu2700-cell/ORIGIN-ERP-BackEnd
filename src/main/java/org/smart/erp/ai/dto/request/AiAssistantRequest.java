package org.smart.erp.ai.dto.request;

public record AiAssistantRequest(
        Long conversationId,
        String message
) {
}

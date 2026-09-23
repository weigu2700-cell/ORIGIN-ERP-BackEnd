package org.smart.erp.ai.request;

public record AiAssistantRequest(
        Long conversationId,
        String message
) {
}

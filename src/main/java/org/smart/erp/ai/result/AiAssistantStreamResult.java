package org.smart.erp.ai.result;



public record AiAssistantStreamResult(
        AiStreamType type,
        String content
) {
}

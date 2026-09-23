package org.smart.erp.ai.result;


public record AiAssistantEventResult<T>(
        AiStreamType type,
        T data
) {
}

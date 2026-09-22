package org.smart.erp.ai.result;

import org.smart.erp.ai.enums.AiStreamType;

public record AiAssistantStreamResult(
        AiStreamType type,
        String content
) {
}

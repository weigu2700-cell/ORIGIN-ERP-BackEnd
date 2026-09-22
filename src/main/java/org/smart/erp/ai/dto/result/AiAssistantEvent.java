package org.smart.erp.ai.dto.result;

import org.apache.poi.ss.formula.functions.T;
import org.smart.erp.ai.enums.AiStreamType;

public record AiAssistantEvent(
        AiStreamType type,
        T data
) {
}

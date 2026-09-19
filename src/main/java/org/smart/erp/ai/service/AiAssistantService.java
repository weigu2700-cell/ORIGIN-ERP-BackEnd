package org.smart.erp.ai.service;

import org.smart.erp.ai.dto.request.AiAssistantRequest;
import org.smart.erp.ai.dto.result.AiAssistantResult;

public interface AiAssistantService {
    AiAssistantResult chat(AiAssistantRequest request);

}

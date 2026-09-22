package org.smart.erp.ai.assistant.service;

import org.smart.erp.ai.assistant.dto.AiAssistantRequest;
import org.smart.erp.ai.assistant.dto.AiAssistantResult;
import org.smart.erp.ai.assistant.dto.AiAssistantStreamResult;
import reactor.core.publisher.Flux;

public interface AiAssistantService {
    AiAssistantResult chat(AiAssistantRequest request);

    Flux<AiAssistantStreamResult> chatStream(AiAssistantRequest request);

}

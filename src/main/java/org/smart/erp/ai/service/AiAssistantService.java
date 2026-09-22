package org.smart.erp.ai.service;

import org.smart.erp.ai.request.AiAssistantRequest;
import org.smart.erp.ai.result.AiAssistantResult;
import org.smart.erp.ai.result.AiAssistantStreamResult;
import reactor.core.publisher.Flux;

public interface AiAssistantService {
    AiAssistantResult chat(AiAssistantRequest request);

    Flux<AiAssistantStreamResult> chatStream(AiAssistantRequest request);

}

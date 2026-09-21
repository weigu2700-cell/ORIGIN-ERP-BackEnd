package org.smart.erp.ai.service;

import org.smart.erp.ai.dto.request.AiAssistantRequest;
import org.smart.erp.ai.dto.result.AiAssistantResult;
import org.smart.erp.ai.dto.result.ConversationTitleResult;
import reactor.core.publisher.Flux;

public interface AiAssistantService {
    AiAssistantResult chat(AiAssistantRequest request);

    Flux<AiAssistantResult> chatStream(AiAssistantRequest request);

    ConversationTitleResult updateChat(AiAssistantRequest request);
}

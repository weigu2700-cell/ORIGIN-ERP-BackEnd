package org.smart.erp.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.ai.dto.request.AiAssistantRequest;
import org.smart.erp.ai.dto.result.AiAssistantResult;
import org.smart.erp.ai.service.AiAssistantService;
import org.smart.erp.common.result.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/ai/assistant")
@Tag(name = "AI助手接口",description = "AI助手接口")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    @Operation(description = "AI助手对话")
    public Result<AiAssistantResult> chat(
            @RequestBody @Parameter(description = "AI助手请求参数") AiAssistantRequest request) {
        return Result.success(aiAssistantService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(description = "AI助手对话流式接口")
    public Flux<AiAssistantResult> chatStream(
            @RequestBody @Parameter(description = "AI助手请求参数") AiAssistantRequest request) {
        return aiAssistantService.chatStream(request);
    }
}

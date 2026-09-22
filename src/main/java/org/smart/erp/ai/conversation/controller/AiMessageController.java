package org.smart.erp.ai.conversation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.ai.conversation.entity.AiMessage;
import org.smart.erp.ai.conversation.service.AiMessageService;
import org.smart.erp.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/message")
@Tag(name = "AI消息接口",description = "AI消息接口")
public class AiMessageController {

    private final AiMessageService aiMessageService;

    public AiMessageController(AiMessageService aiMessageService) {
        this.aiMessageService = aiMessageService;
    }

    @GetMapping("/{conversationId}")
    @Operation(description = "获取历史AI消息")
    public Result<List<AiMessage>> list(
            @PathVariable @Parameter(description = "对话ID") Long conversationId) {
        return Result.success(aiMessageService.listMessage(conversationId));
    }
}

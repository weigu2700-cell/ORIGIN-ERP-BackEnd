package org.smart.erp.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.ai.dto.request.ConversationResult;
import org.smart.erp.ai.entity.AiConversation;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/conversation")
@Tag(name = "AI对话接口",description = "AI对话框")
public class AiConversationController {

    private final AiConversationService aiConversationService;

    public AiConversationController(
            AiConversationService aiConversationService
    ) {
        this.aiConversationService = aiConversationService;
    }

    @PostMapping
    @Operation(description = "创建AI对话")
    public Result<ConversationResult> add() {
        return Result.success(aiConversationService.addConversation());
    }

    @GetMapping("/{conversationId}")
    @Operation(description = "获取AI对话")
    public Result<AiConversation> get(
            @PathVariable @Parameter(description = "对话ID") Long conversationId) {
        return Result.success(aiConversationService.getOwnedConversation(conversationId));
    }

    @GetMapping
    @Operation(description = "获取AI对话列表")
    public Result<List<AiConversation>> list() {
        return Result.success(aiConversationService.listConversation());
    }

    @PutMapping("/{conversationId}/archive")
    @Operation(description = "归档AI对话")
    public Result<ConversationResult> archive(
            @RequestBody @Parameter(description = "对话ID") Long conversationId) {
        return Result.success(aiConversationService.archiveConversation(conversationId));
    }
}

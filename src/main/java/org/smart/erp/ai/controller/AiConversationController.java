package org.smart.erp.ai.controller;

import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/conversation")
public class AiConversationController {

    private final AiConversationService aiConversationService;

    public AiConversationController(
            AiConversationService aiConversationService
    ) {
        this.aiConversationService = aiConversationService;
    }

    @PostMapping
    public Result<Void> add() {
        aiConversationService.addConversation();
        return Result.success();
    }
}

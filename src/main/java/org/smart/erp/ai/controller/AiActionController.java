package org.smart.erp.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.ai.service.AiActionService;
import org.smart.erp.common.result.Result;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/action")
@Tag(name = "AI待确认操作接口", description = "执行用户明确确认后的 AI 业务操作")
public class AiActionController {

    private final AiActionService aiActionService;
    private final CurrentUser currentUser;

    public AiActionController(AiActionService aiActionService, CurrentUser currentUser) {
        this.aiActionService = aiActionService;
        this.currentUser = currentUser;
    }

    @PostMapping("/{token}/execute")
    @Operation(summary = "确认并执行 AI 待确认操作")
    public Result<Void> execute(@PathVariable @Parameter(description = "操作唯一标识") String token) {
        aiActionService.executePendingAction(token, currentUser.getUserId());
        return Result.success();
    }
}

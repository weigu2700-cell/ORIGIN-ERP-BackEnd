package org.smart.erp.ai.service.impl;

import org.smart.erp.ai.request.AiAssistantRequest;
import org.smart.erp.ai.result.ConversationTitleResult;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.ai.persistence.AiConversation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AiConversationTitleService {

    private final ChatClient chatClientTitle;
    private final AiConversationService aiConversationService;

    public AiConversationTitleService(ChatClient.Builder builder,
                                      AiConversationService aiConversationService) {
        this.chatClientTitle = builder.clone().build();
        this.aiConversationService = aiConversationService;
    }

    public String generateTitleIfNeeded(AiAssistantRequest request, SecurityContext securityContext) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(securityContext);
        try {
            AiConversation aiConversation = aiConversationService.getOwnedConversation(request.conversationId());
            if (!"新对话".equals(aiConversation.getTitle())) {
                return null;
            }
            ConversationTitleResult result = chatClientTitle.prompt()
                    .system("请根据用户的对话生成不超过 20 字的中文标题，仅返回 JSON，例如 {\"title\":\"库存查询\"}。")
                    .user(request.message())
                    .call()
                    .entity(ConversationTitleResult.class);
            if (result == null || result.title() == null || result.title().isBlank()) {
                return null;
            }
            String title = result.title().trim();
            aiConversationService.writeTitle(request.conversationId(), title);
            return title;
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

}

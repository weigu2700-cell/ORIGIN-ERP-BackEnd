package org.smart.erp.ai.assistant.service.impl;

import org.smart.erp.ai.action.collector.AiActionCollector;
import org.smart.erp.ai.assistant.dto.AiAssistantRequest;
import org.smart.erp.ai.assistant.dto.AiAssistantResult;
import org.smart.erp.ai.assistant.dto.AiAssistantStreamResult;
import org.smart.erp.ai.assistant.dto.ConversationTitleResult;
import org.smart.erp.ai.conversation.entity.AiConversation;
import org.smart.erp.ai.assistant.model.AiStreamType;
import org.smart.erp.ai.assistant.service.AiAssistantService;
import org.smart.erp.ai.conversation.service.AiConversationService;
import org.smart.erp.ai.conversation.service.AiMessageService;
import org.smart.erp.ai.assistant.config.AiAssistantPrompt;
import org.smart.erp.ai.tool.AiQueryTools;
import org.smart.erp.ai.tool.ToolExecutionSupport;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    private final ChatClient chatClient;
    private final ChatClient chatClientTitle;
    private final AiQueryTools queryTools;
    private final CurrentUser currentUser;
    private final AiMessageService aiMessageService;
    private final AiConversationService aiConversationService;
    public AiAssistantServiceImpl(
            ChatClient.Builder builder,
            AiQueryTools queryTools,
            CurrentUser currentUser,
            ChatMemory chatMemory,
            AiMessageService aiMessageService,
            AiConversationService aiConversationService
    ) {
        this.chatClientTitle = builder.clone().build();
        this.chatClient = builder
                .clone()
                .defaultSystem(AiAssistantPrompt.SYSTEM)
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build()
                ).build();
        this.queryTools = queryTools;
        this.currentUser = currentUser;
        this.aiMessageService = aiMessageService;
        this.aiConversationService = aiConversationService;
    }

    @Override
    public AiAssistantResult chat(AiAssistantRequest request) {
        if (request.conversationId() == null) {
            throw new BusinessException(400, "找不到对话");
        }
        Long conversationId = request.conversationId();
        aiMessageService.addMessage(conversationId, "user", request.message());
        try {
            String content = chatClient
                    .prompt()
                    .user(request.message())
                    .advisors(a -> a.param(
                            ChatMemory.CONVERSATION_ID,
                            conversationId.toString()
                    ))
                    .tools(queryTools.all())
                    .toolContext(Map.of(
                            "userId", currentUser.getUserId(),
                            ToolExecutionSupport.SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext()
                    ))
                    .call()
                    .content();
            aiMessageService.addMessage(conversationId, "assistant", content);
            return new AiAssistantResult(content);
        } catch (Exception e) {
            log.warn("AI 对话调用失败 conversationId={}", conversationId, e);
            throw new BusinessException(500, "AI 服务暂时不可用，请稍后重试");
        }
    }


    @Override
    public Flux<AiAssistantStreamResult> chatStream(AiAssistantRequest request) {

        if (request.conversationId() == null) {
            return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR,"找不到对话"));
        }

        Long conversationId = request.conversationId();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());

        AiActionCollector aiActionCollector = new AiActionCollector();

        final Map<String, Object> toolContext;
        try {
            toolContext = Map.of(
                    "userId", currentUser.getUserId(),
                    ToolExecutionSupport.SECURITY_CONTEXT_KEY, securityContext,
                    AiActionCollector.ACTION_COLLECTOR_KEY, aiActionCollector
            );
            // 归属校验和写入仍在请求线程完成；失败时返回 SSE 错误，避免响应类型冲突。
            aiMessageService.addMessage(conversationId, "user", request.message());
        } catch (Exception e) {
            log.warn("准备 AI 流式对话失败 conversationId={}", conversationId, e);
            String message = e instanceof BusinessException ? e.getMessage() : "AI 服务暂时不可用，请稍后重试";
            return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR, message));
        }

        return Flux.defer(() -> {
            StringBuilder stringBuilder = new StringBuilder();
            Flux<AiAssistantStreamResult> streamResult =
                    chatClient
                            .prompt()
                            .user(request.message())
                            .advisors(a -> a.param(
                                    ChatMemory.CONVERSATION_ID,
                                    conversationId.toString()
                            ))
                            .tools(queryTools.all())
                            .toolContext(toolContext)
                            .stream()
                            .content()
                            .doOnNext(stringBuilder::append)
                            .map(content ->
                                    new AiAssistantStreamResult(AiStreamType.CONTENT, content)
                            );
            Mono<Void> saveAssistant = Mono.fromRunnable(() -> aiMessageService.saveMessage(
                            conversationId,
                            "assistant",
                            stringBuilder.toString()
                    ))
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnError(error -> log.warn("AI 对话保存失败 conversationId={}", conversationId, error))
                    .onErrorResume(error -> Mono.empty())
                    .then();

            Mono<AiAssistantStreamResult> generateTitle =
                    Mono.fromCallable(() -> {

                        SecurityContext previous = SecurityContextHolder.getContext();
                        SecurityContextHolder.setContext(securityContext);

                        try {
                            return generateTitleIfNeeded(request);
                        } finally {
                            SecurityContextHolder.setContext(previous);
                        }
                    })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(result -> {
                                if (result.title() == null) {
                                    return Mono.empty();
                                }

                                return Mono.just(
                                        new AiAssistantStreamResult(
                                                AiStreamType.TITLE,
                                                result.title()
                                        )
                                );
                            }).doOnError(error ->
                                    log.warn(
                                            "AI 标题生成失败 conversationId={}",
                                            conversationId, error
                                    )
                            )
                            .onErrorResume(error -> Mono.empty());

            return streamResult
                    .concatWith(saveAssistant.then(Mono.empty()))
                    .concatWith(generateTitle)
                    .doFinally(signalType -> {
                        if (signalType == SignalType.CANCEL) {
                            log.info(
                                    "用户主动停止 AI 生成 conversationId={}, partialLength={}",
                                    conversationId,
                                    stringBuilder.length()
                            );
                        }
                    })
                    .doOnError(error ->
                            log.warn("AI 流式对话失败 conversationId={}", conversationId, error))
                    .onErrorResume(error -> {
                        if (!stringBuilder.isEmpty()) {
                            return Flux.just(
                                    new AiAssistantStreamResult(AiStreamType.ERROR,
                                            "\n\n  AI回答中断，请重试"
                                    )
                            );
                        }

                        return Flux.just(
                                new AiAssistantStreamResult(AiStreamType.ERROR,"AI 服务暂时不可用，请稍后重试")
                        );
                    });
        });
    }


    private ConversationTitleResult generateTitleIfNeeded(AiAssistantRequest request) {

        AiConversation aiConversation = aiConversationService.getOwnedConversation(request.conversationId());

        if ( "新对话".equals(aiConversation.getTitle())) {

            ConversationTitleResult conversationTitleResult =
                    chatClientTitle
                            .prompt()
                            .system("请根据用户的对话生成不超过 20 字的中文标题，仅返回 JSON，例如 {\"title\":\"库存查询\"}。")
                            .user(request.message())
                            .call()
                            .entity(ConversationTitleResult.class);

            if (conversationTitleResult != null) {
                aiConversationService.writeTitle(
                        request.conversationId(),
                        conversationTitleResult.title()
                );
                return conversationTitleResult;
            }

        }

        return null;
    }



}

package org.smart.erp.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.smart.erp.ai.service.AiActionService;
import org.smart.erp.ai.service.AiAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smart.erp.ai.request.AiAssistantRequest;
import org.smart.erp.ai.result.AiAssistantResult;
import org.smart.erp.ai.result.AiAssistantStreamResult;
import org.smart.erp.ai.result.AiStreamType;
import org.smart.erp.ai.config.AiAssistantPrompt;
import org.smart.erp.ai.config.AiQueryTools;
import org.smart.erp.ai.service.AiMessageService;
import org.smart.erp.ai.tool.ToolExecutionSupport;
import org.smart.erp.ai.tool.action.AiActionCollector;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);
    private static final String UNAVAILABLE_MESSAGE = "AI 服务暂时不可用，请稍后重试";

    private final ChatClient chatClient;
    private final AiQueryTools queryTools;
    private final CurrentUser currentUser;
    private final AiMessageService aiMessageService;
    private final AiConversationTitleService titleService;
    private final AiActionService aiActionService;
    private final ObjectMapper objectMapper;

    public AiAssistantServiceImpl(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            AiQueryTools queryTools,
            CurrentUser currentUser,
            AiMessageService aiMessageService,
            AiConversationTitleService titleService,
            AiActionService aiActionService,
            ObjectMapper objectMapper
    ) {
        this.chatClient = builder.clone()
                .defaultSystem(AiAssistantPrompt.SYSTEM)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.queryTools = queryTools;
        this.currentUser = currentUser;
        this.aiMessageService = aiMessageService;
        this.titleService = titleService;
        this.aiActionService = aiActionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAssistantResult chat(AiAssistantRequest request) {
        validate(request);
        Long conversationId = request.conversationId();
        // addMessage 校验当前用户拥有该对话，之后才允许模型读取记忆。
        aiMessageService.addMessage(conversationId, "user", request.message());
        try {
            String content = chatClient.prompt()
                    .user(request.message())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
                    .tools(queryTools.all())
                    .toolContext(toolContext(SecurityContextHolder.getContext()))
                    .call()
                    .content();
            aiMessageService.saveMessage(conversationId, "assistant", content);
            return new AiAssistantResult(content);
        } catch (Exception error) {
            log.warn("AI 对话调用失败 conversationId={}", conversationId, error);
            throw new BusinessException(500, UNAVAILABLE_MESSAGE);
        }
    }

    @Override
    public Flux<AiAssistantStreamResult> chatStream(AiAssistantRequest request) {
        try {
            validate(request);
            Long conversationId = request.conversationId();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
            AiActionCollector actionCollector = new AiActionCollector();
            Map<String, Object> toolContext = Map.of(
                    "userId", currentUser.getUserId(),
                    ToolExecutionSupport.SECURITY_CONTEXT_KEY, securityContext,
                    AiActionCollector.ACTION_COLLECTOR_KEY, actionCollector);
            // 在请求线程完成归属校验与用户消息写入。
            aiMessageService.addMessage(conversationId, "user", request.message());
            return streamResponse(request, securityContext, toolContext, actionCollector);
        } catch (Exception error) {
            log.warn("准备 AI 流式对话失败", error);
            String message = error instanceof BusinessException ? error.getMessage() : UNAVAILABLE_MESSAGE;
            return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR, message));
        }
    }

    private Flux<AiAssistantStreamResult> streamResponse(AiAssistantRequest request,
                                                          SecurityContext securityContext,
                                                          Map<String, Object> toolContext,
                                                          AiActionCollector actionCollector) {
        Long conversationId = request.conversationId();
        return Flux.defer(() -> {
            StringBuilder content = new StringBuilder();
            Flux<AiAssistantStreamResult> chunks = chatClient.prompt()
                    .user(request.message())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
                    .tools(queryTools.all())
                    .toolContext(toolContext)
                    .stream()
                    .content()
                    .doOnNext(content::append)
                    .map(chunk -> new AiAssistantStreamResult(AiStreamType.CONTENT, chunk));

            Mono<Void> saveAssistant = Mono.fromRunnable(() -> {
                        if (!content.isEmpty()) {
                            aiMessageService.saveMessage(conversationId, "assistant", content.toString());
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnError(error -> log.warn("AI 对话保存失败 conversationId={}", conversationId, error))
                    .onErrorResume(error -> Mono.empty())
                    .then();

            Mono<AiAssistantStreamResult> title = Mono.fromCallable(() ->
                            titleService.generateTitleIfNeeded(request, securityContext)
                    )
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(value -> Mono.just(new AiAssistantStreamResult(AiStreamType.TITLE, value)))
                    .doOnError(error -> log.warn("AI 标题生成失败 conversationId={}", conversationId, error))
                    .onErrorResume(error -> Mono.empty());

            Flux<AiAssistantStreamResult> actionEvents =
                    Mono.fromCallable(() -> aiActionService.createPendingAction(
                                    actionCollector.getAll(),
                                    (Long) toolContext.get("userId"),
                                    conversationId)
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMapMany(Flux::fromIterable)
                            .map(action -> new AiAssistantStreamResult(
                                    AiStreamType.ACTION,
                                    toJson(action)))
                            .doOnError(error -> log.warn(
                                    "AI 创建待办失败 conversationId={}", conversationId, error))
                            .onErrorResume(error -> Flux.empty());

            return chunks.concatWith(saveAssistant.then(Mono.empty()))
                    .concatWith(title)
                    .concatWith(actionEvents)
                    .doOnCancel(() -> log.info("用户主动停止 AI 生成 conversationId={}, partialLength={}",
                            conversationId, content.length()))
                    .doOnError(error -> log.warn("AI 流式对话失败 conversationId={}", conversationId, error))
                    .onErrorResume(error -> Flux.just(new AiAssistantStreamResult(
                            AiStreamType.ERROR,
                            content.isEmpty() ? UNAVAILABLE_MESSAGE : "\n\nAI回答中断，请重试"))
                    );
        });
    }

    private Map<String, Object> toolContext(SecurityContext securityContext) {
        return Map.of("userId", currentUser.getUserId(),
                ToolExecutionSupport.SECURITY_CONTEXT_KEY, securityContext);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            log.warn("AI 事件序列化失败 type={}", value.getClass().getSimpleName(), error);
            return String.valueOf(value);
        }
    }

    private static void validate(AiAssistantRequest request) {
        if (request == null || request.conversationId() == null) {
            throw new BusinessException(400, "找不到对话");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new BusinessException(400, "消息不能为空");
        }
    }
}

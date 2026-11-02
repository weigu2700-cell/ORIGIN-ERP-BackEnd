package org.smart.erp.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.smart.erp.ai.config.AiActionTool;
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
import org.smart.erp.ai.tool.action.collector.AiActionCollector;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import org.smart.erp.tenant.context.TenantExecutor;
import org.smart.erp.tenant.context.TenantExecution;
import org.smart.erp.tenant.context.TenantKeyspace;
import org.smart.erp.tenant.context.TenantMemoryId;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);
    private static final String UNAVAILABLE_MESSAGE = "AI 服务暂时不可用，请稍后重试";

    private final ChatClient chatClient;
    private final AiQueryTools queryTools;
    private final AiActionTool aiActionTool;
    private final TenantExecutor tenants;
    private final TenantKeyspace tenantKeys;
    private final AiMessageService aiMessageService;
    private final AiConversationTitleService titleService;
    private final AiActionService aiActionService;
    private final ObjectMapper objectMapper;

    public AiAssistantServiceImpl(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            AiQueryTools queryTools,
            AiActionTool aiActionTool,
            TenantExecutor tenants, TenantKeyspace tenantKeys,
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
        this.tenants = tenants;
        this.tenantKeys = tenantKeys;
        this.aiMessageService = aiMessageService;
        this.titleService = titleService;
        this.aiActionService = aiActionService;
        this.objectMapper = objectMapper;
        this.aiActionTool = aiActionTool;
    }

    @Override
    public AiAssistantResult chat(AiAssistantRequest request) {
        validate(request);
        Long conversationId = request.conversationId();
        var execution = tenants.capture();
        // addMessage 校验当前用户拥有该对话，之后才允许模型读取记忆。
        aiMessageService.addMessage(conversationId, "user", request.message());
        try {
            String content = chatClient.prompt()
                    .user(request.message())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, memoryId(execution, conversationId)))
                    .tools(queryTools.all())
                    .toolContext(toolContext(execution))
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
            var execution = tenants.capture();
            AiActionCollector actionCollector = new AiActionCollector();
            Map<String, Object> toolContext = Map.of(
                    "userId", execution.userId(),
                    TenantExecution.CONTEXT_KEY, execution,
                    AiActionCollector.ACTION_COLLECTOR_KEY, actionCollector);
            // 在请求线程完成归属校验与用户消息写入。
            aiMessageService.addMessage(conversationId, "user", request.message());
            return streamResponse(request, execution, toolContext, actionCollector);
        } catch (Exception error) {
            log.warn("准备 AI 流式对话失败", error);
            String message = error instanceof BusinessException ? error.getMessage() : UNAVAILABLE_MESSAGE;
            return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR, message));
        }
    }

    private Flux<AiAssistantStreamResult> streamResponse(AiAssistantRequest request,
                                                          TenantExecution snapshot,
                                                          Map<String, Object> toolContext,
                                                          AiActionCollector actionCollector) {
        Long conversationId = request.conversationId();
        return Flux.deferContextual(context -> {
            TenantExecution execution = context.get(TenantExecution.CONTEXT_KEY);
            StringBuilder content = new StringBuilder();
            Flux<AiAssistantStreamResult> chunks = execution.call(() -> chatClient.prompt()
                    .user(request.message())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, memoryId(execution, conversationId)))
                    .tools(queryTools.all()).tools(aiActionTool.all()).toolContext(toolContext)
                    .stream().content())
                    .doOnNext(content::append)
                    .map(chunk -> new AiAssistantStreamResult(AiStreamType.CONTENT, chunk));
            Mono<Void> saveAssistant = Mono.fromRunnable(() -> execution.run(() -> {
                        if (!content.isEmpty()) aiMessageService.saveMessage(conversationId, "assistant", content.toString());
                    })).subscribeOn(Schedulers.boundedElastic()).then();
            Mono<AiAssistantStreamResult> title = Mono.fromCallable(() -> titleService.generateTitleIfNeeded(request, execution))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(value -> value == null ? Mono.empty() : Mono.just(new AiAssistantStreamResult(AiStreamType.TITLE, value)));
            Flux<AiAssistantStreamResult> actions = Mono.fromCallable(() -> execution.call(() ->
                            aiActionService.createPendingAction(actionCollector.getAll(), execution.userId(), conversationId)))
                    .subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable)
                    .map(action -> new AiAssistantStreamResult(AiStreamType.ACTION, toJson(action)));
            // 模型可能长时间只输出内容而不访问 DB，独立检查企业状态以终止停用后的流。
            Mono<Void> guard = Flux.interval(java.time.Duration.ofSeconds(5)).publishOn(Schedulers.boundedElastic())
                    .doOnNext(tick -> execution.run(() -> {})).then();
            return chunks.concatWith(saveAssistant.then(Mono.empty())).concatWith(title).concatWith(actions)
                    .takeUntilOther(guard)
                    .doOnCancel(() -> log.info("用户停止 AI 生成 tenantId={},conversationId={}", execution.tenantId(), conversationId))
                    .doOnError(error -> log.warn("AI 流式对话失败 tenantId={},conversationId={}", execution.tenantId(), conversationId, error))
                    .onErrorResume(error -> Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR,
                            content.isEmpty() ? UNAVAILABLE_MESSAGE : "\n\nAI回答中断，请重试")));
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.warn("AI 执行企业已不可用 tenantId={},conversationId={}", snapshot.tenantId(), conversationId);
                    return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR, UNAVAILABLE_MESSAGE));
                })
                .contextWrite(context -> context.put(TenantExecution.CONTEXT_KEY, snapshot));
    }

    private Map<String, Object> toolContext(TenantExecution execution) {
        return Map.of("userId", execution.userId(), TenantExecution.CONTEXT_KEY, execution);
    }

    private String memoryId(TenantExecution execution, Long conversationId) {
        return tenantKeys.multi() ? new TenantMemoryId(execution.tenantId(), execution.userId(), conversationId).encode()
                : conversationId.toString();
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

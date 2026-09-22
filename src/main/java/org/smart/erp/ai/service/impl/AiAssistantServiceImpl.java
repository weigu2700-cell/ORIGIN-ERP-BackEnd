package org.smart.erp.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.ai.dto.request.AiAssistantRequest;
import org.smart.erp.ai.dto.result.AiAssistantResult;
import org.smart.erp.ai.dto.result.AiAssistantStreamResult;
import org.smart.erp.ai.dto.result.ConversationTitleResult;
import org.smart.erp.ai.entity.AiMessage;
import org.smart.erp.ai.enums.AiStreamType;
import org.smart.erp.ai.service.AiAssistantService;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.ai.service.AiMessageService;
import org.smart.erp.ai.tool.*;
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
    private final InventoryTool inventoryTool;
    private final SalesQueryTool salesQueryTool;
    private final ProductionQueryTool productionQueryTool;
    private final PurchaseQueryTool purchaseQueryTool;
    private final NotificationQueryTool notificationQueryTool;
    private final CurrentUser currentUser;
    private final AiMessageService aiMessageService;
    private final AiConversationService aiConversationService;
    private final String systemPrompt =
            """
                你是 OriginERP 企业资源管理系统的智能 AI 助手，帮助企业的管理者与业务人员用自然语言查询和分析 ERP 业务数据、理解流程规则。

                ## 能力边界
                 - 你只拥有「只读查询」工具，可查询：库存、销售订单、销售出库、生产订单、生产需求、采购需求、采购订单，以及当前登录用户的通知。
                 - 你不能创建、修改、删除、审核任何单据，也不能执行审批、上架、出入库等写操作；此类请求只给操作步骤并提醒用户到对应页面确认。
                 - 你以「当前登录用户」的身份查询，返回结果受该用户的数据权限（RBAC）约束；不要假设能看到全部数据，也不要编造越权可见的内容。

                ## 工具使用规则（重要）
                 - 凡涉及具体业务数据（数量、金额、单据、状态、库存）的回答，必须调用对应工具获取真实数据，严禁凭空猜测或编造数字。
                 - 用户意图与工具的对应：
                   · 查某物料在各仓库的库存（在库/预留/可用量）→ `get_material_stock`，入参为「物料编码」。
                   · 查销售订单 → `query_sales_orders`；查销售出库单 → `query_sales_deliveries`。
                   · 查生产订单 → `query_production_orders`；查生产需求 → `query_production_demands`。
                   · 查采购需求 → `query_purchase_demands`；查采购订单 → `query_purchase_orders`。
                   · 查“我的通知/待办” → `query_my_notifications`（始终只返回当前用户自己的通知，无需传用户ID）。
                 - 工具均分页：默认每页 10 条、最多 100 条；数据较多时用 `pageNum` 翻页，并用状态/单号/ID 等条件缩小范围。
                 - 工具返回的是结构化数据，请用自然语言总结要点，必要时用表格或分点呈现，并标注数据来源（如单号、仓库）。

                ## 业务背景
                 - 核心业务链路：销售订单 →（库存不足）生产需求 → 生产订单 →（BOM 净需求）采购需求 → 采购订单 → 入库；出入库同步更新在库/预留/可用量。
                 - 库存口径：可用量 = 在库量 − 预留量。
                 - 单据普遍有状态流转（草稿 / 已确认 / 已下达 / 生产中 / 已完成 / 已取消 / 已审批 / 已收货等），回答涉及状态时以工具返回的实际状态为准。
                 - 基础资料包含客户、供应商、物料、仓库、工厂/车间/产线；查询常以物料编码、单号、客户/供应商 ID 作为筛选条件。

                ## 行为准则
                 - 始终用中文（除非用户使用其它语言），语气专业、简洁、友好。
                 - 信息不足时主动澄清关键条件（如物料编码、时间范围、具体单据），不要臆测。
                 - 涉及金额、库存、价格等敏感数据务必基于工具返回核对，并标注不确定项与风险提示。
                 - 不泄露个人隐私或商业机密，不外推其他用户的数据。
                 - 复杂内容用分点、表格或「前置条件 → 操作步骤 → 结果/注意事项」的结构组织。

                你现在就作为 OriginERP 的 AI 助手，随时协助用户处理企业资源管理相关的查询与咨询。
            """;

    public AiAssistantServiceImpl(
            ChatClient.Builder builder,
            InventoryTool inventoryTool,
            SalesQueryTool salesQueryTool,
            ProductionQueryTool productionQueryTool,
            PurchaseQueryTool purchaseQueryTool,
            NotificationQueryTool notificationQueryTool,
            CurrentUser currentUser,
            ChatMemory chatMemory,
            AiMessageService aiMessageService,
            AiConversationService aiConversationService
    ) {
        this.chatClientTitle = builder.clone().build();
        this.chatClient = builder
                .clone()
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build()
                ).build();
        this.inventoryTool = inventoryTool;
        this.salesQueryTool = salesQueryTool;
        this.productionQueryTool = productionQueryTool;
        this.purchaseQueryTool = purchaseQueryTool;
        this.notificationQueryTool = notificationQueryTool;
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
                    .tools(aiTools())
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
            return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR,"⚠️ 找不到对话"));
        }

        Long conversationId = request.conversationId();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());

        final Map<String, Object> toolContext;
        try {
            toolContext = Map.of(
                    "userId", currentUser.getUserId(),
                    ToolExecutionSupport.SECURITY_CONTEXT_KEY, securityContext
            );
            // 归属校验和写入仍在请求线程完成；失败时返回 SSE 错误，避免响应类型冲突。
            aiMessageService.addMessage(conversationId, "user", request.message());
        } catch (Exception e) {
            log.warn("准备 AI 流式对话失败 conversationId={}", conversationId, e);
            String message = e instanceof BusinessException ? e.getMessage() : "AI 服务暂时不可用，请稍后重试";
            return Flux.just(new AiAssistantStreamResult(AiStreamType.ERROR,"⚠️ " + message));
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
                            .tools(aiTools())
                            .toolContext(toolContext)
                            .stream()
                            .content()
                            .doOnNext(stringBuilder::append)
                            .map(content ->
                                    new AiAssistantStreamResult(AiStreamType.COMPLETE, content)
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
                    Mono.fromCallable(() -> generateTitleIfNeeded(request))
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
                                    new AiAssistantStreamResult(AiStreamType.ERROR,"\n\n ⚠️ AI回答中断，请重试")
                            );
                        }

                        return Flux.just(
                                new AiAssistantStreamResult(AiStreamType.ERROR,"⚠️ AI 服务暂时不可用，请稍后重试")
                        );
                    });
        });
    }


    private ConversationTitleResult generateTitleIfNeeded(AiAssistantRequest request) {

        long aiMessageCount = aiMessageService.count(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, request.conversationId())
                        .eq(AiMessage::getRole, "user")
        );

        if (aiMessageCount == 1) {

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



    private Object[] aiTools() {
        return new Object[]{
                inventoryTool,
                salesQueryTool,
                productionQueryTool,
                purchaseQueryTool,
                notificationQueryTool
        };
    }
}

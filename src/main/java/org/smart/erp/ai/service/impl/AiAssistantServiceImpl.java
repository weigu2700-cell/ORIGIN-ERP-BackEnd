package org.smart.erp.ai.service.impl;

import org.smart.erp.ai.dto.request.AiAssistantRequest;
import org.smart.erp.ai.dto.result.AiAssistantResult;
import org.smart.erp.ai.service.AiAssistantService;
import org.smart.erp.ai.service.AiMessageService;
import org.smart.erp.ai.tool.InventoryTool;
import org.smart.erp.ai.tool.NotificationQueryTool;
import org.smart.erp.ai.tool.ProductionQueryTool;
import org.smart.erp.ai.tool.PurchaseQueryTool;
import org.smart.erp.ai.tool.SalesQueryTool;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    private final ChatClient chatClient;
    private final InventoryTool inventoryTool;
    private final SalesQueryTool salesQueryTool;
    private final ProductionQueryTool productionQueryTool;
    private final PurchaseQueryTool purchaseQueryTool;
    private final NotificationQueryTool notificationQueryTool;
    private final CurrentUser currentUser;
    private final AiMessageService aiMessageService;
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
            AiMessageService aiMessageService
    ) {
        this.chatClient = builder
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
                    .toolContext(Map.of("userId", currentUser.getUserId()))
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
    public Flux<AiAssistantResult> chatStream(AiAssistantRequest request) {
        if (request.conversationId() == null) {
            throw new BusinessException(404, "找不到对话");
        }
        return Flux.defer(() -> {
            StringBuilder contentBuilder = new StringBuilder();
            Flux<AiAssistantResult> chatStream = chatClient
                    .prompt()
                    .user(request.message())
                    .advisors(a -> a.param(
                            ChatMemory.CONVERSATION_ID,
                            request.conversationId().toString()
                    ))
                    .tools(aiTools())
                    .toolContext(Map.of("userId", currentUser.getUserId()))
                    .stream()
                    .content()
                    .doOnNext(contentBuilder::append)
                    .map(AiAssistantResult::new);
            return Flux.concat(
                    Mono.fromRunnable(
                            () -> aiMessageService.addMessage(
                                    request.conversationId(),
                                    "user",
                                    request.message()
                            ))
                            .subscribeOn(Schedulers.boundedElastic()).then(Mono.empty()),
                    chatStream
            ).concatWith(
                    Mono.fromRunnable(
                            () -> aiMessageService.addMessage(
                                    request.conversationId(),
                                    "assistant",
                                    contentBuilder.toString()
                            )
                    ).subscribeOn(Schedulers.boundedElastic()).then(Mono.empty())
            ).onErrorResume(
                    t -> Flux.just(
                            new AiAssistantResult("AI 服务暂时不可用，请稍后重试")
                    ));
        });
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
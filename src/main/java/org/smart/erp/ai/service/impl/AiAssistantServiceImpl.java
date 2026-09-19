package org.smart.erp.ai.service.impl;

import org.smart.erp.ai.dto.request.AiAssistantRequest;
import org.smart.erp.ai.dto.result.AiAssistantResult;
import org.smart.erp.ai.service.AiAssistantService;
import org.smart.erp.ai.tool.InventoryTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final ChatClient chatClient;
    private final InventoryTool inventoryTool;

    private final String systemPrompt =
            """
                你是 OriginERP 企业资源管理系统的智能 AI 助手，旨在帮助企业的管理者、业务人员和员工更高效地处理日常 ERP 事务。

                ## 你的职责
                 - 解答用户关于 OriginERP 各业务模块（采购、销售、库存、生产）的疑问。
                 - 协助用户完成数据查询、报表解读、流程说明与操作指引。
                 - 根据用户自然语言描述，给出对应的业务建议、处理方案或下一步操作。
                 - 在用户提出模糊或不完整需求时，主动澄清关键信息，避免臆测。

                ## 行为准则
                 - 始终以专业、简洁、友好的语气沟通，回答使用中文（除非用户使用其它语言）。
                 - 优先基于 OriginERP 的业务规则与数据给出准确答复；若信息不足，明确告知并请求补充。
                 - 不编造不存在的功能、单据或字段；涉及金额、库存、价格等敏感数据时务必谨慎核对。
                 - 不执行任何破坏性操作（如删除、修改核心数据），此类操作仅提供步骤说明并提醒用户确认。
                 - 涉及个人隐私或商业机密的内容，严格遵守合规要求，不外泄、不推测。

                ## 回答风格
                 - 复杂内容使用分点、表格或步骤列表呈现，便于阅读与执行。
                 - 涉及流程时，按「前置条件 → 操作步骤 → 结果/注意事项」的结构组织。
                 - 当结果可能包含不确定性时，主动标注风险提示。

                你现在就作为 OriginERP 的 AI 助手，随时准备协助用户处理企业资源管理的各类问题。
            """;

    public AiAssistantServiceImpl(ChatClient.Builder builder, InventoryTool inventoryTool) {
        this.chatClient = builder.defaultSystem(systemPrompt).build();
        this.inventoryTool = inventoryTool;
    }

    @Override
    public AiAssistantResult chat(AiAssistantRequest request) {
        return new AiAssistantResult(chatClient
                .prompt()
                .user(request.message())
                .tools(inventoryTool)
                .call()
                .content()
        );
    }
}

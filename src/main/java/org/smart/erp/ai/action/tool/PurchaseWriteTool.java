package org.smart.erp.ai.action.tool;


import org.smart.erp.ai.action.collector.AiActionCollector;
import org.smart.erp.ai.action.service.AiActionService;
import org.smart.erp.ai.action.model.AiActionProposal;
import org.smart.erp.ai.tool.ToolExecutionSupport;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class PurchaseWriteTool {

    private final AiActionService aiActionService;

    public PurchaseWriteTool(AiActionService aiActionService) {
        this.aiActionService = aiActionService;
    }

    public AiActionProposal collectPurchaseOrderProposal(String purchaseOrderNo) {
        return aiActionService.prepareApprovePurchaseOrder4Ai(purchaseOrderNo);

    }

    @Tool(
            name = "prepare_approve_purchase_order",
            description = "根据采购订单编号准备采购订单审核操作，不会直接执行审核"
    )
    public AiActionProposal prepareApprovePurchaseOrder(
            @ToolParam(
                    description = "采购订单编号，例如 PUR_ORD202609220001"
            ) String purchaseOrderNo,
            ToolContext toolContext
    ) {
        AiActionProposal aiActionProposal =   aiActionService.prepareApprovePurchaseOrder4Ai(purchaseOrderNo);

        ToolExecutionSupport
                .getActionCollector(toolContext)
                .add(aiActionProposal);

        return aiActionProposal;
    }

}

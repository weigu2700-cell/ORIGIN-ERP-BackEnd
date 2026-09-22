package org.smart.erp.ai.action.tool;


import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.action.collector.AiActionCollector;
import org.smart.erp.ai.action.service.AiActionService;
import org.smart.erp.ai.action.model.AiActionProposal;
import org.smart.erp.ai.tool.ToolExecutionSupport;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseWriteTool {

    private final AiActionService aiActionService;

    public AiActionProposal collectPurchaseOrderProposal(String purchaseOrderNo) {
        return aiActionService.prepareApprovePurchaseOrder4Ai(purchaseOrderNo);

    }

    @Tool(
            name = "prepare_approve_purchase_order",
            description = "根据采购订单编号准备采购订单审核操作，不会直接执行审核"
    )
    public AiActionProposal prepareApprovePurchaseOrder(
            @ToolParam(required = true, description = "采购订单编号") String purchaseOrderNo,
            ToolContext toolContext
    ) {
        AiActionProposal proposal =
                aiActionService.prepareApprovePurchaseOrder4Ai(purchaseOrderNo);

        AiActionCollector collector = ToolExecutionSupport.getActionCollector(toolContext);

        collector.add(proposal);

        return proposal;
    }

}

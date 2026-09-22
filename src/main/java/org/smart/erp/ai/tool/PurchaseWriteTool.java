package org.smart.erp.ai.tool;


import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.action.Service.impl.AiActionServiceImpl;
import org.smart.erp.ai.action.collector.AiActionCollector;
import org.smart.erp.ai.action.model.AiActionProposal;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseWriteTool {

    private final AiActionServiceImpl aiActionService;

    public AiActionProposal collectPurchaseOrderProposal(Long id) {
        AiActionProposal aiActionProposal = aiActionService.prepareApprovePurchaseOrder4Ai(id);
        AiActionCollector collector = new AiActionCollector();

        collector.add(aiActionProposal);
        return aiActionProposal;
    }
}

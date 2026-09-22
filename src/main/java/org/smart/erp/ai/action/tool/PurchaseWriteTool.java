package org.smart.erp.ai.action.tool;


import org.smart.erp.ai.action.service.AiActionService;
import org.smart.erp.ai.action.model.AiActionProposal;

/** Proposal draft. Register as an AI tool only after confirmation and execution are implemented. */
public class PurchaseWriteTool {

    private final AiActionService aiActionService;

    public PurchaseWriteTool(AiActionService aiActionService) {
        this.aiActionService = aiActionService;
    }

    public AiActionProposal collectPurchaseOrderProposal(Long id) {
        return aiActionService.prepareApprovePurchaseOrder4Ai(id);
    }
}

package org.smart.erp.ai.service;

import org.smart.erp.ai.model.AiActionProposal;
import org.smart.erp.ai.result.AiPendingActionResult;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface AiActionService {

    @PreAuthorize("hasAnyAuthority('purchase:order:approve')")
    AiActionProposal prepareApprovePurchaseOrder4Ai(String purchaseOrderNo);

    List<AiPendingActionResult> createPendingAction(
            List<AiActionProposal> proposals,
            Long userId,
            Long conversationId
    );
}

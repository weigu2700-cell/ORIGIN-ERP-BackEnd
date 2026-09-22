package org.smart.erp.ai.action.Service;

import org.smart.erp.ai.action.model.AiActionProposal;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AiActionService {

    @PreAuthorize("hasAnyAuthority('purchase:order:get')")
    AiActionProposal prepareApprovePurchaseOrder4Ai(Long id);
}

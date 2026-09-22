package org.smart.erp.ai.action.service;

import org.smart.erp.ai.action.model.AiActionProposal;
import org.springframework.security.access.prepost.PreAuthorize;

/** Builds a proposal only; executing the business action requires a separate flow. */
public interface AiActionService {

    @PreAuthorize("hasAnyAuthority('purchase:order:get')")
    AiActionProposal prepareApprovePurchaseOrder4Ai(Long id);
}

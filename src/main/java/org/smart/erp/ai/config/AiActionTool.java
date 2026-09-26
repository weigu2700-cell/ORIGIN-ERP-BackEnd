package org.smart.erp.ai.config;

import org.smart.erp.ai.tool.action.PurchaseWriteTool;
import org.springframework.stereotype.Component;

@Component
public class AiActionTool {

    private final PurchaseWriteTool purchaseWriteTool;

    public AiActionTool(PurchaseWriteTool purchaseWriteTool) {
        this.purchaseWriteTool = purchaseWriteTool;
    }

    public Object[] all() {
        return new Object[]{purchaseWriteTool};
    }
}

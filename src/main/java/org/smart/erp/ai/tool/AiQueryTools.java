package org.smart.erp.ai.tool;

import org.springframework.stereotype.Component;

/** Single allowlist of tools exposed to the read-only assistant. */
@Component
public class AiQueryTools {

    private final InventoryTool inventory;
    private final SalesQueryTool sales;
    private final ProductionQueryTool production;
    private final PurchaseQueryTool purchase;
    private final NotificationQueryTool notification;

    public AiQueryTools(InventoryTool inventory, SalesQueryTool sales,
                        ProductionQueryTool production, PurchaseQueryTool purchase,
                        NotificationQueryTool notification) {
        this.inventory = inventory;
        this.sales = sales;
        this.production = production;
        this.purchase = purchase;
        this.notification = notification;
    }

    public Object[] all() {
        return new Object[]{inventory, sales, production, purchase, notification};
    }
}

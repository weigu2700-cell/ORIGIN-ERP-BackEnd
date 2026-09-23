package org.smart.erp.ai.config;

import org.smart.erp.ai.tool.query.InventoryTool;
import org.smart.erp.ai.tool.query.NotificationQueryTool;
import org.smart.erp.ai.tool.query.ProductionQueryTool;
import org.smart.erp.ai.tool.query.PurchaseQueryTool;
import org.smart.erp.ai.tool.query.SalesQueryTool;
import org.springframework.stereotype.Component;

/** 模型可调用的只读工具白名单。新增工具必须显式登记。 */
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

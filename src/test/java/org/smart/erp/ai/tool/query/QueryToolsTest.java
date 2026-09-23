package org.smart.erp.ai.tool.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.ai.tool.ToolExecutionSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.smart.erp.eip.service.NotificationInboxService;
import org.smart.erp.production.service.ProductionDemandService;
import org.smart.erp.production.service.ProductionOrderService;
import org.smart.erp.purchase.service.PurchaseDemandService;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.smart.erp.sales.dto.salesOrderDto.SalesOrderPageDto;
import org.smart.erp.sales.enums.SalesOrderStatus;
import org.smart.erp.sales.service.SalesDeliveryService;
import org.smart.erp.sales.service.SalesOrderService;
import org.smart.erp.sales.vo.SalesOrderVo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class QueryToolsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersSevenTypedQueriesWithoutExposingSecurityContext() {
        ToolCallback[] callbacks = ToolCallbacks.from(
                new SalesQueryTool(stub(SalesOrderService.class, (proxy, method, args) -> null),
                        stub(SalesDeliveryService.class, (proxy, method, args) -> null)),
                new ProductionQueryTool(stub(ProductionOrderService.class, (proxy, method, args) -> null),
                        stub(ProductionDemandService.class, (proxy, method, args) -> null)),
                new PurchaseQueryTool(stub(PurchaseDemandService.class, (proxy, method, args) -> null),
                        stub(PurchaseOrderService.class, (proxy, method, args) -> null)),
                new NotificationQueryTool(stub(NotificationInboxService.class, (proxy, method, args) -> null))
        );

        assertEquals(Set.of("query_sales_orders", "query_sales_deliveries", "query_production_orders",
                "query_production_demands", "query_purchase_demands", "query_purchase_orders",
                "query_my_notifications"), Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name()).collect(Collectors.toSet()));
        for (ToolCallback callback : callbacks) {
            String schema = callback.getToolDefinition().inputSchema();
            assertTrue(schema.contains("pageNum"), callback.getToolDefinition().name());
            assertFalse(schema.contains("toolContext"), callback.getToolDefinition().name());
            assertFalse(schema.contains("securityContext"), callback.getToolDefinition().name());
            assertFalse(callback.getToolDefinition().description().isBlank());
        }
    }

    @Test
    void salesToolBindsTypedStatusAndRestoresCallerSecurityContext() {
        AtomicReference<SalesOrderPageDto> capturedQuery = new AtomicReference<>();
        AtomicReference<SecurityContext> observedContext = new AtomicReference<>();
        SalesOrderService service = stub(SalesOrderService.class, (proxy, method, args) -> {
            capturedQuery.set((SalesOrderPageDto) args[0]);
            observedContext.set(SecurityContextHolder.getContext());
            return new Page<SalesOrderVo>(1, 5);
        });
        SalesQueryTool salesTool = new SalesQueryTool(service,
                stub(SalesDeliveryService.class, (proxy, method, args) -> null));
        ToolCallback callback = Arrays.stream(ToolCallbacks.from(salesTool))
                .filter(tool -> tool.getToolDefinition().name().equals("query_sales_orders"))
                .findFirst().orElseThrow();

        SecurityContext caller = SecurityContextHolder.createEmptyContext();
        caller.setAuthentication(new UsernamePasswordAuthenticationToken("caller", "n/a"));
        SecurityContext execution = SecurityContextHolder.createEmptyContext();
        execution.setAuthentication(new UsernamePasswordAuthenticationToken("tool-user", "n/a"));
        SecurityContextHolder.setContext(caller);

        callback.call("{\"customerId\":42,\"status\":1,\"pageSize\":5}",
                new ToolContext(Map.of(ToolExecutionSupport.SECURITY_CONTEXT_KEY, execution)));

        assertEquals(42L, capturedQuery.get().getCustomerId());
        assertEquals(SalesOrderStatus.CONFIRMED, capturedQuery.get().getStatus());
        assertEquals(1, capturedQuery.get().getPageNum());
        assertEquals(5, capturedQuery.get().getPageSize());
        assertSame(execution, observedContext.get());
        assertSame(caller, SecurityContextHolder.getContext());
    }

    @Test
    void missingSecurityContextAndOversizedPageAreRejected() {
        AtomicBoolean called = new AtomicBoolean();
        SalesOrderService service = stub(SalesOrderService.class, (proxy, method, args) -> {
            called.set(true);
            return null;
        });
        SalesQueryTool salesTool = new SalesQueryTool(service,
                stub(SalesDeliveryService.class, (proxy, method, args) -> null));
        assertThrows(SecurityException.class, () -> salesTool.querySalesOrders(
                null, null, null, null, null, new ToolContext(Map.of())));
        assertThrows(IllegalArgumentException.class, () -> salesTool.querySalesOrders(
                null, null, null, 1, 101, new ToolContext(Map.of())));
        PurchaseQueryTool purchaseTool = new PurchaseQueryTool(
                stub(PurchaseDemandService.class, (proxy, method, args) -> null),
                stub(PurchaseOrderService.class, (proxy, method, args) -> null));
        NotificationQueryTool notificationTool = new NotificationQueryTool(
                stub(NotificationInboxService.class, (proxy, method, args) -> null));
        assertThrows(IllegalArgumentException.class, () -> purchaseTool.queryPurchaseOrders(
                null, null, null, null, 1, 101, new ToolContext(Map.of())));
        assertThrows(IllegalArgumentException.class, () -> notificationTool.queryMyNotifications(
                null, 1, 101, new ToolContext(Map.of())));
        assertFalse(called.get());
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}

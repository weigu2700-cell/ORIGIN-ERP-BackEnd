package org.smart.erp.ai.tool;

import org.smart.erp.ai.tool.action.AiActionCollector;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * Restores the caller's authentication around tool execution, including execution
 * performed on a thread different from the request thread.
 */
public final class ToolExecutionSupport {

    public static final String SECURITY_CONTEXT_KEY = "securityContext";

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private ToolExecutionSupport() {
    }

    /**
     * 归一化分页参数：缺省使用首页与默认页大小，越界则抛 {@link IllegalArgumentException}。
     * 供各查询工具复用，避免在每个工具里重复实现分页校验。
     */
    public static int normalizePage(Integer pageNum) {
        if (pageNum == null) {
            return DEFAULT_PAGE;
        }
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        return pageNum;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页条数必须在1到100之间");
        }
        return pageSize;
    }

    public static <T> T withSecurityContext(ToolContext toolContext, Supplier<T> action) {
        if (toolContext == null || action == null) {
            throw new IllegalArgumentException("toolContext 和 action 不能为空");
        }

        Object value = toolContext.getContext().get(SECURITY_CONTEXT_KEY);
        if (!(value instanceof SecurityContext securityContext)
                || securityContext.getAuthentication() == null) {
            throw new SecurityException("缺少安全上下文，无法执行工具调用");
        }

        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(securityContext);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    public static AiActionCollector getActionCollector(ToolContext toolContext) {
        Object value = toolContext.getContext()
                .get(AiActionCollector.ACTION_COLLECTOR_KEY);

        if (!(value instanceof AiActionCollector collector)) {
            throw new IllegalStateException("AiActionCollector not found in ToolContext");
        }

        return collector;
    }
}

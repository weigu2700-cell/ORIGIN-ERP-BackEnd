package org.smart.erp.tenant.context;

import org.smart.erp.tenant.TenantException;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Supplier;

/**
 * 按企业隔离的模型对话记忆。
 *
 * <p>advisor 可能在模型工作线程读写记忆，因此这里按显式的 memory ID 恢复租户作用域，
 * 不依赖线程继承。</p>
 */
public final class TenantJdbcChatMemory implements ChatMemory {

    private static final String OWNERSHIP_SQL = "SELECT COUNT(*) FROM ai_conversation"
            + " WHERE id=? AND user_id=? AND deleted=0 AND status=0";
    private static final int MAX_MESSAGES = 20;

    private final DataSource source;
    private final TenantExecutor tenants;
    private volatile ChatMemory delegate;

    public TenantJdbcChatMemory(DataSource source, TenantExecutor tenants) {
        this.source = source;
        this.tenants = tenants;
    }

    private ChatMemory delegate() {
        ChatMemory current = delegate;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (delegate == null) {
                // Spring AI 2.0 的 builder 会探测数据库方言；延迟到明确企业作用域后再创建，
                // 避免在应用启动时获取业务连接。
                JdbcChatMemoryRepository repository = JdbcChatMemoryRepository.builder()
                        .dataSource(source)
                        .build();
                delegate = MessageWindowChatMemory.builder()
                        .chatMemoryRepository(repository)
                        .maxMessages(MAX_MESSAGES)
                        .build();
            }
            return delegate;
        }
    }

    private <T> T within(String value, Supplier<T> action) {
        TenantMemoryId memoryId = TenantMemoryId.parse(value);
        return tenants.forTenant(memoryId.tenantId()).call(() -> {
            Integer owned = new JdbcTemplate(source).queryForObject(
                    OWNERSHIP_SQL,
                    Integer.class,
                    memoryId.conversationId(),
                    memoryId.userId()
            );
            if (!Integer.valueOf(1).equals(owned)) {
                throw new TenantException(403, "模型记忆会话不存在、已归档或不属于当前用户");
            }
            return action.get();
        });
    }

    @Override
    public void add(String id, List<Message> messages) {
        within(id, () -> {
            delegate().add(id, messages);
            return null;
        });
    }

    @Override
    public List<Message> get(String id) {
        return within(id, () -> delegate().get(id));
    }

    @Override
    public void clear(String id) {
        within(id, () -> {
            delegate().clear(id);
            return null;
        });
    }
}

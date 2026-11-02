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

/** advisor 可能在模型工作线程读写记忆，按显式 memory ID 恢复作用域，不依赖线程继承。 */
public final class TenantJdbcChatMemory implements ChatMemory {
    private final DataSource source;
    private final TenantExecutor tenants;
    private volatile ChatMemory delegate;
    public TenantJdbcChatMemory(DataSource source, TenantExecutor tenants) { this.source = source; this.tenants = tenants; }
    private ChatMemory delegate() {
        var current = delegate;
        if (current != null) return current;
        synchronized (this) {
            if (delegate == null) {
                // Spring AI 2.0 的 builder 会探测方言；延迟至明确企业作用域内，不在启动时取业务连接。
                var repository = JdbcChatMemoryRepository.builder().dataSource(source).build();
                delegate = MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(20).build();
            }
            return delegate;
        }
    }
    private <T> T within(String value, Supplier<T> action) {
        var id = TenantMemoryId.parse(value);
        return tenants.forTenant(id.tenantId()).call(() -> {
            Integer owned = new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM ai_conversation WHERE id=? AND user_id=? AND deleted=0 AND status=0", Integer.class, id.conversationId(), id.userId());
            if (!Integer.valueOf(1).equals(owned)) throw new TenantException(403, "模型记忆会话不存在、已归档或不属于当前用户");
            return action.get();
        });
    }
    @Override public void add(String id, List<Message> messages) { within(id, () -> { delegate().add(id, messages); return null; }); }
    @Override public List<Message> get(String id) { return within(id, () -> delegate().get(id)); }
    @Override public void clear(String id) { within(id, () -> { delegate().clear(id); return null; }); }
}

package org.smart.erp.im;

import org.smart.erp.tenant.context.TenantKeyspace;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 显式 SQL 和行锁保证消息/成员/事件的原子性；数据源仍由现有租户路由提供。 */
@Repository
public class ImStore {

    private final JdbcTemplate jdbc;

    private final TenantKeyspace keys;

    public ImStore(DataSource source, TenantKeyspace keys) {
        this.jdbc = new JdbcTemplate(source);
        this.keys = keys;
    }

    public String tenant() {
        return keys.tenantId();
    }

    public List<Map<String, Object>> rows(String sql, Object... args) {
        tenant();
        return jdbc.queryForList(sql, args);
    }

    public Map<String, Object> one(String sql, Object... args) {
        var rows = rows(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public int update(String sql, Object... args) {
        tenant();
        return jdbc.update(sql, args);
    }

    public long insert(String sql, Object... args) {
        tenant();
        var holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++)
                statement.setObject(i + 1, args[i]);
            return statement;
        }, holder);
        return java.util.Objects.requireNonNull(holder.getKey()).longValue();
    }

    public Map<String, Object> conversation(long id, boolean lock) {
        return one("SELECT * FROM im_conversation WHERE tenant_id=? AND id=?" + (lock ? " FOR UPDATE" : ""), tenant(),
                id);
    }

    public Map<String, Object> member(long conversation, long user) {
        return one("SELECT * FROM im_member WHERE tenant_id=? AND conversation_id=? AND user_id=? AND state='ACTIVE'",
                tenant(), conversation, user);
    }

    public List<Long> members(long conversation) {
        return rows(
                "SELECT user_id FROM im_member WHERE tenant_id=? AND conversation_id=? AND state='ACTIVE' ORDER BY user_id",
                tenant(), conversation)
            .stream()
            .map(r -> n(r, "user_id"))
            .toList();
    }

    public static long n(Map<String, Object> row, String key) {
        var value = row.get(key);
        return value == null ? 0 : ((Number) value).longValue();
    }

    public static int i(Map<String, Object> row, String key) {
        return Math.toIntExact(n(row, key));
    }

    public static String s(Map<String, Object> row, String key) {
        var value = row.get(key);
        return value == null ? null : value.toString();
    }

    public static String id(Map<String, Object> row, String key) {
        return row.get(key) == null ? null : Long.toString(n(row, key));
    }

    public static Instant instant(Map<String, Object> row, String key) {
        var value = row.get(key);
        if (value instanceof Timestamp time)
            return time.toInstant();
        if (value instanceof java.time.LocalDateTime time)
            return time.toInstant(java.time.ZoneOffset.UTC);
        if (value instanceof Instant time)
            return time;
        throw new IllegalStateException("缺少IM时间字段: " + key);
    }

    public static String time(Map<String, Object> row, String key) {
        return instant(row, key).toString();
    }

    public static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

}

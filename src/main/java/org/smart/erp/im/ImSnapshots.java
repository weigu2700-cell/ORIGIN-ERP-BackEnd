package org.smart.erp.im;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static org.smart.erp.im.ImModels.*;
import static org.smart.erp.im.ImStore.*;

@Service
public class ImSnapshots {

    public record Cached(List<Conversation> records, String syncEventSeq, int totalUnreadCount, String expiresAt) {
    }

    private final ImStore db;

    private final ImAccess access;

    private final ImViews views;

    private final ImEvents events;

    private final ImTransactions tx;

    private final ImRedis redis;

    private final ImJson json;

    public ImSnapshots(ImStore db, ImAccess access, ImViews views, ImEvents events, ImTransactions tx, ImRedis redis,
            ImJson json) {
        this.db = db;
        this.access = access;
        this.views = views;
        this.events = events;
        this.tx = tx;
        this.redis = redis;
        this.json = json;
    }

    public Snapshot list(String token, String cursor, int limit) {
        long user = access.actor();
        if ((token == null) != (cursor == null))
            throw ImException.invalid("后续分页须同时提供snapshotToken和cursor");
        return tx.read(() -> {
            String selected = token;
            Cached cached;
            int offset = 0;
            if (selected == null) {
                String high = Long.toString(events.high(user));
                var rows = db.rows(
                        "SELECT conversation_id FROM im_member WHERE tenant_id=? AND user_id=? AND state='ACTIVE' ORDER BY conversation_id DESC LIMIT 1001",
                        db.tenant(), user);
                if (rows.size() > 1000)
                    throw new ImException(503, "CHAT_UNAVAILABLE", "会话数量超出当前快照容量，请联系管理员");
                var conversations = rows.stream()
                    .map(r -> views.conversation(n(r, "conversation_id"), user))
                    .sorted(Comparator.comparing(Conversation::updatedAt).reversed().thenComparing(Conversation::id))
                    .toList();
                cached = new Cached(conversations, high,
                        conversations.stream().mapToInt(Conversation::unreadCount).sum(),
                        Instant.now().plus(Duration.ofMinutes(10)).toString());
                selected = UUID.randomUUID().toString();
                redis.put(redis.key(db.tenant(), "snapshot:" + user + ":" + selected), json.write(cached),
                        Duration.ofMinutes(10));
            }
            else {
                ImValues.uuid(selected);
                String value = redis.get(redis.key(db.tenant(), "snapshot:" + user + ":" + selected));
                if (value == null)
                    throw expired();
                cached = json.read(value, Cached.class);
                long parsed = ImValues.seq(cursor);
                if (parsed > cached.records().size())
                    throw ImException.invalid("快照分页游标无效");
                offset = Math.toIntExact(parsed);
            }
            if (!Instant.parse(cached.expiresAt()).isAfter(Instant.now()))
                throw expired();
            int end = Math.min(offset + limit, cached.records().size());
            var records = new ArrayList<Conversation>();
            for (var conversation : cached.records().subList(offset, end)) {
                var member = db.member(ImValues.id(conversation.id()), user);
                if (member == null)
                    continue;
                if (n(member, "visible_from_seq") != ImValues.seq(conversation.visibleFromSeq()))
                    throw expired();
                records.add(conversation);
            }
            return new Snapshot(records, selected, end < cached.records().size() ? Integer.toString(end) : null,
                    end < cached.records().size(), cached.syncEventSeq(), cached.totalUnreadCount(),
                    cached.expiresAt());
        });
    }

    private ImException expired() {
        return new ImException(410, "SNAPSHOT_EXPIRED", "会话快照已过期或访问窗口变化，请重新获取");
    }

}

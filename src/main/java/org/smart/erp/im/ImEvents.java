package org.smart.erp.im;

import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static org.smart.erp.im.ImModels.*;
import static org.smart.erp.im.ImStore.*;

@Service
public class ImEvents {

    public record Emission(String type, Object payload, List<Long> recipients) {
    }

    private final ImStore db;

    private final ImJson json;

    private final ImAccess access;

    private final ImViews views;

    private final ImTransactions tx;

    public ImEvents(ImStore db, ImJson json, ImAccess access, ImViews views, ImTransactions tx) {
        this.db = db;
        this.json = json;
        this.access = access;
        this.views = views;
        this.tx = tx;
    }

    /** 调用者持会话锁；先按固定顺序锁全部收件人，再分配多个事件，避免跨会话死锁。 */
    public Map<Long, Long> append(long conversation, long visibleSeq, List<Emission> emissions) {
        var recipients = new TreeSet<Long>();
        emissions.forEach(e -> recipients.addAll(e.recipients()));
        for (long user : recipients) {
            db.update(
                    "INSERT INTO im_user_stream(tenant_id,user_id) VALUES(?,?) ON DUPLICATE KEY UPDATE user_id=user_id",
                    db.tenant(), user);
            db.one("SELECT last_seq FROM im_user_stream WHERE tenant_id=? AND user_id=? FOR UPDATE", db.tenant(), user);
        }
        var firstSequence = new HashMap<Long, Long>();
        for (var emission : emissions) {
            long event = db.insert(
                    "INSERT INTO im_event(tenant_id,type,conversation_id,visibility_seq,payload) VALUES(?,?,?,?,?)",
                    db.tenant(), emission.type(), conversation, visibleSeq, json.write(emission.payload()));
            for (long user : new TreeSet<>(emission.recipients())) {
                db.update("UPDATE im_user_stream SET last_seq=last_seq+1 WHERE tenant_id=? AND user_id=?", db.tenant(),
                        user);
                long seq = high(user);
                firstSequence.putIfAbsent(user, seq);
                db.update("INSERT INTO im_user_event(tenant_id,user_id,event_seq,event_id) VALUES(?,?,?,?)",
                        db.tenant(), user, seq, event);
                db.update("INSERT INTO im_outbox(tenant_id,event_id,recipient_user_id) VALUES(?,?,?)", db.tenant(),
                        event, user);
            }
        }
        return firstSequence;
    }

    public long high(long user) {
        var row = db.one("SELECT last_seq FROM im_user_stream WHERE tenant_id=? AND user_id=?", db.tenant(), user);
        return row == null ? 0 : n(row, "last_seq");
    }

    public EventPage sync(long after, int limit) {
        return syncUser(access.actor(), after, limit);
    }

    public EventPage syncUser(long user, long after, int limit) {
        access.require(user, "im:access");
        return tx.read(() -> {
            var stream = db.one("SELECT * FROM im_user_stream WHERE tenant_id=? AND user_id=?", db.tenant(), user);
            long high = stream == null ? 0 : n(stream, "last_seq");
            long floor = stream == null ? 0 : n(stream, "pruned_through_seq");
            var expired = db.one(
                    "SELECT MAX(event_seq) last_expired FROM im_user_event WHERE tenant_id=? AND user_id=? AND created_at<?",
                    db.tenant(), user, Timestamp.from(Instant.now().minus(Duration.ofDays(7))));
            floor = Math.max(floor, n(expired, "last_expired"));
            if (after < floor)
                throw new ImException(410, "SYNC_CURSOR_EXPIRED", "同步游标已过期，请重建会话快照");
            if (after > high)
                throw ImException.invalid("同步游标超过当前用户水位");
            var rows = db.rows(
                    """
                            SELECT e.*,ue.event_seq FROM im_user_event ue JOIN im_event e ON e.id=ue.event_id AND e.tenant_id=ue.tenant_id
                            WHERE ue.tenant_id=? AND ue.user_id=? AND ue.event_seq>? ORDER BY ue.event_seq LIMIT ?
                            """,
                    db.tenant(), user, after, limit + 1);
            boolean more = rows.size() > limit;
            var result = new ArrayList<Event>();
            for (var row : rows.subList(0, Math.min(limit, rows.size())))
                result.add(deliver(row, user));
            String next = result.isEmpty() ? Long.toString(after) : result.getLast().eventSeq();
            return new EventPage(result, next, more, Long.toString(high));
        });
    }

    private Event deliver(Map<String, Object> row, long user) {
        String type = s(row, "type");
        Object payload;
        var member = db.member(n(row, "conversation_id"), user);
        boolean inWindow = member != null && n(row, "visibility_seq") >= n(member, "visible_from_seq");
        boolean revoked = "CONVERSATION.REVOKED".equals(type);
        if ((!revoked && !inWindow) || (revoked && member != null && !inWindow)) {
            type = "EVENT.SKIPPED";
            payload = Map.of("reason", "ACCESS_CHANGED");
        }
        else if ("CONVERSATION.UPDATED".equals(type)) {
            payload = Map.of("conversation", views.conversation(n(row, "conversation_id"), user));
        }
        else
            payload = json.object(s(row, "payload"));
        return new Event(1, id(row, "id"), id(row, "event_seq"), type, id(row, "conversation_id"),
                time(row, "created_at"), payload);
    }

}

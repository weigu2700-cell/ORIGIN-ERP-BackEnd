package org.smart.erp.im;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.smart.erp.im.ImModels.*;
import static org.smart.erp.im.ImStore.*;
import static org.smart.erp.im.ImValues.*;

@Service
public class ImService {

    private final ImStore db;

    private final ImAccess access;

    private final ImViews views;

    private final ImEvents events;

    private final ImJson json;

    private final ImTransactions tx;

    private final ImProperties properties;

    private final ImRedis redis;

    public ImService(ImStore db, ImAccess access, ImViews views, ImEvents events, ImJson json, ImTransactions tx,
            ImProperties properties, ImRedis redis) {
        this.db = db;
        this.access = access;
        this.views = views;
        this.events = events;
        this.json = json;
        this.tx = tx;
        this.properties = properties;
        this.redis = redis;
    }

    public Config config() {
        access.actor();
        return new Config(properties.fileLimit(), properties.groupLimit(), 4000, 16384, 1,
                properties.getAllowedExtensions(), 7, Math.clamp(properties.getMaxConnectionsPerUser(), 1, 5));
    }

    public ContactPage contacts(String keyword, String cursor, int limit) {
        long user = access.actor();
        long after = cursor == null ? 0 : ImValues.id(cursor);
        if (keyword != null && keyword.length() > 100)
            throw ImException.invalid("搜索词过长");
        String search = keyword == null ? "" : keyword.strip();
        // 使用相同权限来源筛选员工，不授予系统用户管理权限，也不暴露完整用户行。
        var eligible = db.rows("""
                SELECT u.id,COALESCE(NULLIF(u.real_name,''),u.username) display_name,d.name department_name
                FROM sys_user u LEFT JOIN sys_dept d ON d.id=u.dept_id AND d.deleted=0
                WHERE u.id>? AND u.id<>? AND u.status=1 AND u.deleted=0
                AND (?='' OR LOCATE(?,u.real_name)>0 OR LOCATE(?,u.username)>0)
                AND EXISTS (SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
                  JOIN sys_permission p ON p.code='im:access' AND p.status=1 AND p.deleted=0
                  WHERE ur.user_id=u.id AND r.status=1 AND r.deleted=0 AND (r.code='admin' OR EXISTS
                    (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id)))
                ORDER BY u.id LIMIT ?
                """, after, user, search, search, search, limit + 1);
        boolean more = eligible.size() > limit;
        var records = eligible.subList(0, Math.min(limit, eligible.size()))
            .stream()
            .map(r -> new Contact(id(r, "id"), s(r, "display_name"), s(r, "department_name"), null))
            .toList();
        return new ContactPage(records, more ? records.getLast().userId() : null, more);
    }

    public Conversation conversation(long conversation) {
        long user = access.actor();
        access.conversation(conversation, user, false);
        return views.conversation(conversation, user);
    }

    public Conversation direct(CreateDirect request) {
        long user = access.actor();
        long peer = ImValues.id(request.peerUserId());
        if (peer == user)
            throw ImException.invalid("不能与自己建立单聊");
        if (!access.can(peer, "im:access"))
            throw ImException.missing();
        return tx.write(() -> {
            var existing = db.one(
                    "SELECT id FROM im_conversation WHERE tenant_id=? AND direct_user_low=? AND direct_user_high=?",
                    db.tenant(), Math.min(user, peer), Math.max(user, peer));
            if (existing != null)
                return views.conversation(n(existing, "id"), user);
            long conversation = db.insert(
                    "INSERT INTO im_conversation(tenant_id,type,creator_id,direct_user_low,direct_user_high) VALUES(?,'DIRECT',?,?,?)",
                    db.tenant(), user, Math.min(user, peer), Math.max(user, peer));
            addMember(conversation, user, null, 0);
            addMember(conversation, peer, null, 0);
            events.append(conversation, 0,
                    List.of(new ImEvents.Emission("CONVERSATION.UPDATED", Map.of(), List.of(user, peer))));
            return views.conversation(conversation, user);
        });
    }

    public Conversation createGroup(CreateGroup request) {
        long user = access.actor();
        access.require(user, "im:group:create");
        String key = uuid(request.clientRequestId()), name = name(request.name());
        List<Long> targets = targets(request.memberIds(), user, 2, properties.groupLimit() - 1);
        String hash = json.hash(List.of(name, targets));
        return tx.write(() -> {
            var old = db.one("SELECT * FROM im_conversation WHERE tenant_id=? AND creator_id=? AND client_request_id=?",
                    db.tenant(), user, key);
            if (old != null) {
                if (!hash.equals(s(old, "create_payload_hash")))
                    throw ImException.conflict("IDEMPOTENCY_CONFLICT", "建群标识已对应其他内容");
                return views.conversation(n(old, "id"), user);
            }
            targets.forEach(this::eligible);
            long conversation = db.insert(
                    "INSERT INTO im_conversation(tenant_id,type,name,owner_id,creator_id,client_request_id,create_payload_hash) VALUES(?,'GROUP',?,?,?,?,?)",
                    db.tenant(), name, user, user, key, hash);
            addMember(conversation, user, "OWNER", 1);
            targets.forEach(t -> addMember(conversation, t, "MEMBER", 1));
            long seq = system(conversation, user, "CREATED", targets, "创建群聊");
            broadcast(conversation, seq, List.of(), null);
            return views.conversation(conversation, user);
        });
    }

    public Preference preference(long conversation, PreferenceInput request) {
        long user = access.actor();
        if (request.muted() == null)
            throw ImException.invalid("muted必填");
        return tx.write(() -> {
            var row = access.conversation(conversation, user, true);
            db.update("UPDATE im_member SET muted=? WHERE tenant_id=? AND conversation_id=? AND user_id=?",
                    request.muted() ? 1 : 0, db.tenant(), conversation, user);
            events.append(conversation, n(row, "last_seq"),
                    List.of(new ImEvents.Emission("CONVERSATION.UPDATED", Map.of(), List.of(user))));
            return new Preference(Long.toString(conversation), request.muted());
        });
    }

    public SendResult send(long conversation, SendMessage request) {
        long user = access.actor();
        String client = uuid(request.clientMessageId());
        String type = request.type();
        if ("TEXT".equals(type)) {
            String text = request.text();
            if (request.fileId() != null || text == null || text.isBlank()
                    || text.codePointCount(0, text.length()) > 4000
                    || text.getBytes(StandardCharsets.UTF_8).length > 16384)
                throw ImException.invalid("文本不能为空，最多4000字符/16KiB，且不能带fileId");
        }
        else if ("FILE".equals(type) || "IMAGE".equals(type)) {
            if (request.text() != null)
                throw ImException.invalid("附件消息不能带text");
            ImValues.id(request.fileId());
            access.require(user, "im:file:upload");
        }
        else
            throw ImException.invalid("消息类型仅支持TEXT/FILE/IMAGE");
        String hash = json.hash(Arrays.asList(Long.toString(conversation), type, request.text(), request.fileId()));
        return tx.write(() -> {
            var row = access.conversation(conversation, user, true);
            var member = access.member(conversation, user);
            var old = db.one("SELECT * FROM im_message WHERE tenant_id=? AND sender_id=? AND client_message_id=?",
                    db.tenant(), user, client);
            if (old != null) {
                if (!hash.equals(s(old, "payload_hash")))
                    throw ImException.conflict("IDEMPOTENCY_CONFLICT", "发送标识已对应其他内容");
                if (n(old, "seq") < n(member, "visible_from_seq"))
                    throw ImException.missing();
                return new SendResult(views.message(old), true, id(old, "sender_event_seq"));
            }
            access.writable(row);
            redis.rate(db.tenant(), user, "send", properties.getSendsPerSecond());
            Long fileId = null;
            if (!"TEXT".equals(type)) {
                fileId = ImValues.id(request.fileId());
                var file = db.one(
                        "SELECT f.*,u.expires_at FROM im_file f JOIN im_upload u ON u.id=f.upload_id AND u.tenant_id=f.tenant_id WHERE f.tenant_id=? AND f.id=? FOR UPDATE",
                        db.tenant(), fileId);
                if (file == null || n(file, "conversation_id") != conversation || n(file, "uploader_id") != user)
                    throw ImException.missing();
                if (file.get("message_id") != null)
                    throw ImException.conflict("FILE_ALREADY_USED", "附件已被消息使用");
                if (!"READY".equals(s(file, "state")) || !instant(file, "expires_at").isAfter(java.time.Instant.now()))
                    throw ImException.conflict("FILE_NOT_READY", "附件尚不可发送");
                if ("IMAGE".equals(type) && !s(file, "mime").startsWith("image/"))
                    throw ImException.invalid("IMAGE只支持已校验图片");
            }
            long seq = n(row, "last_seq") + 1;
            long message = db.insert(
                    "INSERT INTO im_message(tenant_id,conversation_id,seq,sender_id,client_message_id,payload_hash,type,text,file_id) VALUES(?,?,?,?,?,?,?,?,?)",
                    db.tenant(), conversation, seq, user, client, hash, type, request.text(), fileId);
            if (fileId != null)
                db.update("UPDATE im_file SET message_id=? WHERE tenant_id=? AND id=?", message, db.tenant(), fileId);
            advance(conversation, seq);
            db.update(
                    "UPDATE im_member SET unread_count=unread_count+1 WHERE tenant_id=? AND conversation_id=? AND state='ACTIVE' AND user_id<>?",
                    db.tenant(), conversation, user);
            var saved = db.one("SELECT * FROM im_message WHERE tenant_id=? AND id=?", db.tenant(), message);
            var receivers = db.members(conversation);
            var sequences = events.append(conversation, seq, List.of(
                    new ImEvents.Emission("MESSAGE.CREATED", Map.of("message", views.message(saved)), receivers),
                    new ImEvents.Emission("CONVERSATION.UPDATED", Map.of(), receivers)));
            db.update("UPDATE im_message SET sender_event_seq=? WHERE tenant_id=? AND id=?", sequences.get(user),
                    db.tenant(), message);
            return new SendResult(views.message(saved), false, Long.toString(sequences.get(user)));
        });
    }

    public Message byClient(String client) {
        long user = access.actor();
        client = uuid(client);
        var row = db.one("SELECT * FROM im_message WHERE tenant_id=? AND sender_id=? AND client_message_id=?",
                db.tenant(), user, client);
        if (row == null)
            throw ImException.missing();
        var member = access.member(n(row, "conversation_id"), user);
        if (n(row, "seq") < n(member, "visible_from_seq"))
            throw ImException.missing();
        return views.message(row);
    }

    public MessagePage history(long conversation, String before, String after, int limit) {
        long user = access.actor();
        if (before != null && after != null)
            throw ImException.invalid("beforeSeq与afterSeq不能同时传");
        long bound = after != null ? seq(after) : before != null ? seq(before) : Long.MAX_VALUE;
        return tx.read(() -> {
            var row = access.conversation(conversation, user, false);
            var member = access.member(conversation, user);
            boolean forward = after != null;
            var rows = new ArrayList<>(db.rows(
                    "SELECT * FROM im_message WHERE tenant_id=? AND conversation_id=? AND seq>=? AND seq"
                            + (forward ? ">" : "<") + "? ORDER BY seq " + (forward ? "ASC" : "DESC") + " LIMIT ?",
                    db.tenant(), conversation, n(member, "visible_from_seq"), bound, limit + 1));
            boolean more = rows.size() > limit;
            if (more)
                rows.removeLast();
            if (!forward)
                Collections.reverse(rows);
            var records = rows.stream().map(views::message).toList();
            String next = records.isEmpty() ? null : (forward ? records.getLast() : records.getFirst()).seq();
            return new MessagePage(records, more, next, forward ? "FORWARD" : "BACKWARD", id(row, "last_seq"));
        });
    }

    public ReadState read(long conversation, ReadInput request) {
        long user = access.actor(), through = seq(request.throughSeq());
        return tx.write(() -> {
            var row = access.conversation(conversation, user, true);
            var member = access.member(conversation, user);
            if (through > n(row, "last_seq") || through < n(member, "visible_from_seq"))
                throw ImException.invalid("已读序号超出可见范围");
            long next = Math.max(through, n(member, "last_read_seq"));
            int unread = i(db.one(
                    "SELECT COUNT(*) total FROM im_message WHERE tenant_id=? AND conversation_id=? AND seq>? AND seq>=? AND sender_id<>? AND type<>'SYSTEM'",
                    db.tenant(), conversation, next, n(member, "visible_from_seq"), user), "total");
            db.update(
                    "UPDATE im_member SET last_read_seq=?,unread_count=? WHERE tenant_id=? AND conversation_id=? AND user_id=?",
                    next, unread, db.tenant(), conversation, user);
            var state = new ReadState(Long.toString(conversation), Long.toString(next), unread);
            events.append(conversation, n(row, "last_seq"),
                    List.of(new ImEvents.Emission("READ.UPDATED", state, List.of(user))));
            return state;
        });
    }

    public MemberPage members(long conversation, String cursor, Integer version, int limit) {
        long user = access.actor(), after = cursor == null ? 0 : ImValues.id(cursor);
        return tx.read(() -> {
            var row = access.conversation(conversation, user, false);
            requireGroup(row);
            if (cursor != null && version == null)
                throw ImException.invalid("成员翻页须提供groupVersion");
            if (version != null && version != i(row, "version"))
                throw ImException.conflict("VERSION_CONFLICT", "成员已变化，请从第一页重取");
            var rows = db.rows(
                    """
                            SELECT m.*,COALESCE(NULLIF(u.real_name,''),u.username) display_name,d.name department_name
                            FROM im_member m JOIN sys_user u ON u.id=m.user_id LEFT JOIN sys_dept d ON d.id=u.dept_id AND d.deleted=0
                            WHERE m.tenant_id=? AND m.conversation_id=? AND m.state='ACTIVE' AND m.user_id>? ORDER BY m.user_id LIMIT ?
                            """,
                    db.tenant(), conversation, after, limit + 1);
            boolean more = rows.size() > limit;
            var records = rows.subList(0, Math.min(limit, rows.size()))
                .stream()
                .map(r -> new GroupMember(id(r, "user_id"), s(r, "display_name"), s(r, "department_name"), null,
                        s(r, "role"), time(r, "joined_at")))
                .toList();
            return new MemberPage(records, more ? records.getLast().userId() : null, more, i(row, "version"));
        });
    }

    public Conversation rename(long conversation, UpdateGroup request) {
        long user = access.actor();
        String name = name(request.name());
        return tx.write(() -> {
            var row = group(conversation, user, request.expectedVersion(), false);
            db.update("UPDATE im_conversation SET name=?,version=version+1 WHERE tenant_id=? AND id=?", name,
                    db.tenant(), conversation);
            long seq = system(conversation, user, "RENAMED", List.of(), "修改群名");
            broadcast(conversation, seq, List.of(), null);
            return views.conversation(conversation, user);
        });
    }

    public Conversation add(long conversation, AddMembers request) {
        long user = access.actor();
        var targets = targets(request.memberIds(), user, 1, 50);
        return tx.write(() -> {
            group(conversation, user, request.expectedVersion(), false);
            if (db.members(conversation).size() + targets.size() > properties.groupLimit())
                throw ImException.conflict("GROUP_FULL", "群人数已达上限");
            for (long target : targets) {
                eligible(target);
                if (db.member(conversation, target) != null)
                    throw ImException.conflict("MEMBER_ALREADY_PRESENT", "员工已在群中");
            }
            long seq = n(db.conversation(conversation, false), "last_seq") + 1;
            targets.forEach(target -> addMember(conversation, target, "MEMBER", seq));
            bump(conversation);
            system(conversation, user, "ADDED", targets, "添加群成员");
            var changes = new ArrayList<ImEvents.Emission>();
            for (long target : targets)
                changes.add(change(conversation, seq, user, target, "ADDED", null, "MEMBER"));
            broadcast(conversation, seq, changes, null);
            return views.conversation(conversation, user);
        });
    }

    public Conversation remove(long conversation, long target, Integer expected) {
        long user = access.actor();
        return tx.write(() -> {
            var row = group(conversation, user, expected, false);
            var victim = access.member(conversation, target);
            if (target == user || "OWNER".equals(s(victim, "role")))
                throw ImException.conflict("OWNER_TRANSFER_REQUIRED", "不能移除自己或群主");
            if (!"OWNER".equals(s(access.member(conversation, user), "role")) && !"MEMBER".equals(s(victim, "role")))
                throw new ImException(403, "GROUP_ROLE_REQUIRED", "管理员只能移除普通成员");
            long seq = removeMember(conversation, user, target, "REMOVED", s(victim, "role"));
            broadcast(conversation, seq,
                    List.of(change(conversation, seq, user, target, "REMOVED", s(victim, "role"), null)), target);
            return views.conversation(conversation, user);
        });
    }

    public Conversation role(long conversation, long target, SetRole request) {
        long user = access.actor();
        if (!List.of("ADMIN", "MEMBER").contains(request.role() == null ? "" : request.role()))
            throw ImException.invalid("role仅支持ADMIN/MEMBER");
        return tx.write(() -> {
            group(conversation, user, request.expectedVersion(), true);
            var member = access.member(conversation, target);
            if ("OWNER".equals(s(member, "role")))
                throw ImException.conflict("OWNER_TRANSFER_REQUIRED", "请使用转让群主接口");
            db.update("UPDATE im_member SET role=? WHERE tenant_id=? AND conversation_id=? AND user_id=?",
                    request.role(), db.tenant(), conversation, target);
            bump(conversation);
            long seq = system(conversation, user, "ROLE_CHANGED", List.of(target), "修改群成员角色");
            broadcast(conversation, seq,
                    List.of(change(conversation, seq, user, target, "ROLE_CHANGED", s(member, "role"), request.role())),
                    null);
            return views.conversation(conversation, user);
        });
    }

    public Conversation transfer(long conversation, TransferOwner request) {
        long user = access.actor(), target = ImValues.id(request.newOwnerId());
        if (target == user)
            throw ImException.invalid("新群主不能是本人");
        return tx.write(() -> {
            group(conversation, user, request.expectedVersion(), true);
            var member = access.member(conversation, target);
            eligible(target);
            db.update("UPDATE im_member SET role='ADMIN' WHERE tenant_id=? AND conversation_id=? AND user_id=?",
                    db.tenant(), conversation, user);
            db.update("UPDATE im_member SET role='OWNER' WHERE tenant_id=? AND conversation_id=? AND user_id=?",
                    db.tenant(), conversation, target);
            db.update("UPDATE im_conversation SET owner_id=?,version=version+1 WHERE tenant_id=? AND id=?", target,
                    db.tenant(), conversation);
            long seq = system(conversation, user, "OWNER_TRANSFERRED", List.of(target), "转让群主");
            broadcast(conversation, seq,
                    List.of(change(conversation, seq, user, user, "ROLE_CHANGED", "OWNER", "ADMIN"),
                            change(conversation, seq, user, target, "OWNER_TRANSFERRED", s(member, "role"), "OWNER")),
                    null);
            return views.conversation(conversation, user);
        });
    }

    public LeftGroup leave(long conversation, VersionInput request) {
        long user = access.actor();
        return tx.write(() -> {
            var row = access.conversation(conversation, user, true);
            requireGroup(row);
            access.writable(row);
            version(row, request.expectedVersion());
            var member = access.member(conversation, user);
            if ("OWNER".equals(s(member, "role")))
                throw ImException.conflict("OWNER_TRANSFER_REQUIRED", "群主须先转让或解散");
            long seq = removeMember(conversation, user, user, "LEFT", s(member, "role"));
            broadcast(conversation, seq,
                    List.of(change(conversation, seq, user, user, "LEFT", s(member, "role"), null)), user);
            return new LeftGroup(Long.toString(conversation), i(row, "version") + 1);
        });
    }

    public Conversation dissolve(long conversation, VersionInput request) {
        long user = access.actor();
        return tx.write(() -> {
            group(conversation, user, request.expectedVersion(), true);
            db.update("UPDATE im_conversation SET status='DISSOLVED',version=version+1 WHERE tenant_id=? AND id=?",
                    db.tenant(), conversation);
            long seq = system(conversation, user, "DISSOLVED", List.of(), "解散群聊");
            broadcast(conversation, seq, List.of(), null);
            return views.conversation(conversation, user);
        });
    }

    private Map<String, Object> group(long conversation, long user, Integer expected, boolean ownerOnly) {
        var row = access.conversation(conversation, user, true);
        requireGroup(row);
        access.writable(row);
        version(row, expected);
        String role = s(access.member(conversation, user), "role");
        if (!"OWNER".equals(role) && (ownerOnly || !"ADMIN".equals(role)))
            throw new ImException(403, "GROUP_ROLE_REQUIRED", "群管理权限不足");
        return row;
    }

    private void requireGroup(Map<String, Object> row) {
        if (!"GROUP".equals(s(row, "type")))
            throw ImException.missing();
    }

    private void version(Map<String, Object> row, Integer expected) {
        if (expected == null || expected < 1)
            throw ImException.invalid("expectedVersion必填且大于0");
        if (expected != i(row, "version"))
            throw ImException.conflict("VERSION_CONFLICT", "群信息已变化，请刷新后重试");
    }

    private List<Long> targets(List<String> values, long user, int min, int max) {
        if (values == null)
            throw ImException.invalid("memberIds必填");
        var targets = values.stream().map(ImValues::id).distinct().sorted().toList();
        if (targets.size() != values.size() || targets.size() < min || targets.size() > max || targets.contains(user))
            throw ImException.invalid("成员列表数量、重复项或本人ID不合法");
        return targets;
    }

    private void eligible(long user) {
        if (!access.can(user, "im:access"))
            throw ImException.missing();
    }

    private void addMember(long conversation, long user, String role, long from) {
        db.update(
                """
                        INSERT INTO im_member(tenant_id,conversation_id,user_id,role,visible_from_seq,last_read_seq) VALUES(?,?,?,?,?,?)
                        ON DUPLICATE KEY UPDATE role=VALUES(role),state='ACTIVE',visible_from_seq=VALUES(visible_from_seq),last_read_seq=VALUES(last_read_seq),
                        unread_count=0,muted=0,joined_at=CURRENT_TIMESTAMP(6),left_at=NULL
                        """,
                db.tenant(), conversation, user, role, from, Math.max(0, from - 1));
    }

    private void bump(long conversation) {
        db.update("UPDATE im_conversation SET version=version+1 WHERE tenant_id=? AND id=?", db.tenant(), conversation);
    }

    private void advance(long conversation, long seq) {
        db.update("UPDATE im_conversation SET last_seq=?,updated_at=CURRENT_TIMESTAMP(6) WHERE tenant_id=? AND id=?",
                seq, db.tenant(), conversation);
    }

    private long system(long conversation, long actor, String action, List<Long> targets, String description) {
        long seq = n(db.conversation(conversation, false), "last_seq") + 1;
        String protocolAction = switch (action) {
            case "CREATED" -> "GROUP_CREATED";
            case "ADDED" -> "MEMBERS_ADDED";
            case "REMOVED" -> "MEMBER_REMOVED";
            case "LEFT" -> "MEMBER_LEFT";
            case "DISSOLVED" -> "GROUP_DISSOLVED";
            case "RENAMED" -> "GROUP_RENAMED";
            default -> action;
        };
        var payload = new SystemPayload(protocolAction, Long.toString(actor),
                targets.stream().map(Object::toString).toList(), description);
        db.update(
                "INSERT INTO im_message(tenant_id,conversation_id,seq,sender_id,payload_hash,type,system_payload) VALUES(?,?,?,?,?,'SYSTEM',?)",
                db.tenant(), conversation, seq, actor, json.hash(payload), json.write(payload));
        advance(conversation, seq);
        return seq;
    }

    private long removeMember(long conversation, long actor, long target, String action, String oldRole) {
        db.update(
                "UPDATE im_member SET state='LEFT',left_at=CURRENT_TIMESTAMP(6),unread_count=0 WHERE tenant_id=? AND conversation_id=? AND user_id=?",
                db.tenant(), conversation, target);
        bump(conversation);
        return system(conversation, actor, action, List.of(target), "LEFT".equals(action) ? "退出群聊" : "移除群成员");
    }

    private ImEvents.Emission change(long conversation, long seq, long actor, long target, String action, String from,
            String to) {
        db.update(
                "INSERT INTO im_member_change(tenant_id,conversation_id,actor_id,target_id,action,from_role,to_role,seq) VALUES(?,?,?,?,?,?,?,?)",
                db.tenant(), conversation, actor, target, action, from, to, seq);
        var payload = new HashMap<String, Object>();
        payload.put("action", action);
        payload.put("userId", Long.toString(target));
        payload.put("role", to);
        payload.put("groupVersion", i(db.conversation(conversation, false), "version"));
        return new ImEvents.Emission("MEMBER.CHANGED", payload, db.members(conversation));
    }

    private void broadcast(long conversation, long seq, List<ImEvents.Emission> changes, Long revoked) {
        var recipients = db.members(conversation);
        var list = new ArrayList<ImEvents.Emission>();
        var message = db.one("SELECT * FROM im_message WHERE tenant_id=? AND conversation_id=? AND seq=?", db.tenant(),
                conversation, seq);
        list.add(new ImEvents.Emission("MESSAGE.CREATED", Map.of("message", views.message(message)), recipients));
        list.addAll(changes);
        list.add(new ImEvents.Emission("CONVERSATION.UPDATED", Map.of(), recipients));
        if (revoked != null)
            list.add(new ImEvents.Emission("CONVERSATION.REVOKED",
                    Map.of("reason",
                            "MEMBER_LEFT".equals(views.message(message).system().action()) ? "LEFT" : "REMOVED"),
                    List.of(revoked)));
        events.append(conversation, seq, list);
    }

}

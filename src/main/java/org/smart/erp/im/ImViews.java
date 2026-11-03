package org.smart.erp.im;

import org.springframework.stereotype.Component;
import java.util.Map;
import static org.smart.erp.im.ImModels.*;
import static org.smart.erp.im.ImStore.*;

@Component
public class ImViews {

    private final ImStore db;

    private final ImJson json;

    public ImViews(ImStore db, ImJson json) {
        this.db = db;
        this.json = json;
    }

    public FileView file(Map<String, Object> row) {
        return new FileView(id(row, "id"), s(row, "name"), i(row, "size"), s(row, "mime"), s(row, "sha256"));
    }

    public Message message(Map<String, Object> row) {
        if (row == null)
            return null;
        var file = row.get("file_id") == null ? null : db
            .one("SELECT * FROM im_file WHERE tenant_id=? AND id=? AND state='READY'", db.tenant(), n(row, "file_id"));
        return new Message(id(row, "id"), id(row, "conversation_id"), id(row, "seq"), id(row, "sender_id"),
                s(row, "client_message_id"), s(row, "type"), s(row, "text"), file == null ? null : file(file),
                row.get("system_payload") == null ? null : json.read(s(row, "system_payload"), SystemPayload.class),
                time(row, "created_at"));
    }

    public Conversation conversation(long conversation, long user) {
        var row = db.conversation(conversation, false);
        var member = db.member(conversation, user);
        if (row == null || member == null)
            throw ImException.missing();
        String name = s(row, "name");
        if ("DIRECT".equals(s(row, "type"))) {
            long peer = n(row, "direct_user_low") == user ? n(row, "direct_user_high") : n(row, "direct_user_low");
            var person = db.one("SELECT COALESCE(NULLIF(real_name,''),username) display_name FROM sys_user WHERE id=?",
                    peer);
            name = person == null ? "已停用员工" : s(person, "display_name");
        }
        var last = db.one(
                "SELECT * FROM im_message WHERE tenant_id=? AND conversation_id=? AND seq>=? ORDER BY seq DESC LIMIT 1",
                db.tenant(), conversation, n(member, "visible_from_seq"));
        int count = i(db.one(
                "SELECT COUNT(*) total FROM im_member WHERE tenant_id=? AND conversation_id=? AND state='ACTIVE'",
                db.tenant(), conversation), "total");
        return new Conversation(id(row, "id"), s(row, "type"), name, s(row, "status"), id(row, "owner_id"),
                i(row, "version"), count, s(member, "role"), id(member, "visible_from_seq"), id(row, "last_seq"),
                id(member, "last_read_seq"), i(member, "unread_count"), n(member, "muted") != 0, message(last),
                time(row, "updated_at"));
    }

}

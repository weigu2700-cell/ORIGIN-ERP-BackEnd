package org.smart.erp.im.attachment;

import org.smart.erp.im.*;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.smart.erp.im.ImModels.*;
import static org.smart.erp.im.ImStore.*;

@Service
public class ImAttachments {

    public record Download(FileView file, InputStream stream, long start, long end,
            boolean partial) implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            stream.close();
        }
    }

    public static class InvalidRange extends ImException {

        public final int size;

        InvalidRange(int size) {
            super(416, "INVALID_RANGE", "仅支持有效的单段字节范围");
            this.size = size;
        }

    }

    private final ImStore db;

    private final ImAccess access;

    private final ImViews views;

    private final ImTransactions tx;

    private final ImJson json;

    private final ImProperties properties;

    private final ImRedis redis;

    private final AttachmentStorage storage;

    private final AttachmentInspector inspector;

    public ImAttachments(ImStore db, ImAccess access, ImViews views, ImTransactions tx, ImJson json,
            ImProperties properties, ImRedis redis, AttachmentStorage storage, AttachmentInspector inspector) {
        this.db = db;
        this.access = access;
        this.views = views;
        this.tx = tx;
        this.json = json;
        this.properties = properties;
        this.redis = redis;
        this.storage = storage;
        this.inspector = inspector;
    }

    public UploadReservation reserve(ReserveUpload request) {
        long user = access.actor();
        access.require(user, "im:file:upload");
        inspector.requireScanner();
        String client = ImValues.uuid(request.clientUploadId());
        long conversation = ImValues.id(request.conversationId());
        inspector.extension(request.name());
        if (request.size() == null || request.size() < 1)
            throw ImException.invalid("文件不能为空");
        if (request.size() > properties.fileLimit())
            throw new ImException(413, "FILE_TOO_LARGE", "文件超过50MB或组织设置的上限");
        if (request.sha256() == null || !request.sha256().matches("[0-9a-f]{64}"))
            throw ImException.invalid("sha256必须为64位小写十六进制");
        if (request.mime() == null || request.mime().isBlank() || request.mime().length() > 255
                || request.mime().chars().anyMatch(c -> c < 32 || c == 127))
            throw ImException.invalid("mime不合法");
        String hash = json
            .hash(Arrays.asList(conversation, request.name(), request.size(), request.mime(), request.sha256()));
        return tx.write(() -> {
            access.writable(access.conversation(conversation, user, true));
            var old = db.one(
                    "SELECT * FROM im_upload WHERE tenant_id=? AND uploader_id=? AND client_upload_id=? FOR UPDATE",
                    db.tenant(), user, client);
            if (old != null) {
                if (!hash.equals(s(old, "payload_hash")))
                    throw ImException.conflict("IDEMPOTENCY_CONFLICT", "上传标识已对应其他文件");
                requireInitiated(old);
                return sign(old);
            }
            redis.rate(db.tenant(), user, "upload", 3);
            db.update("INSERT INTO im_storage_budget(tenant_id) VALUES(?) ON DUPLICATE KEY UPDATE tenant_id=tenant_id",
                    db.tenant());
            var budget = db.one("SELECT reserved_bytes FROM im_storage_budget WHERE tenant_id=? FOR UPDATE",
                    db.tenant());
            if (n(budget, "reserved_bytes") + request.size() > properties.getStorageQuotaBytes())
                throw new ImException(429, "QUOTA_EXCEEDED", "组织附件容量已满");
            int count = i(
                    db.one("""
                            SELECT COUNT(*) total FROM im_upload u LEFT JOIN im_file f ON f.upload_id=u.id AND f.tenant_id=u.tenant_id
                            WHERE u.tenant_id=? AND u.uploader_id=? AND u.state IN ('INITIATED','VERIFYING','READY') AND u.expires_at>CURRENT_TIMESTAMP(6) AND f.message_id IS NULL
                            """,
                            db.tenant(), user),
                    "total");
            if (count >= properties.getMaxPendingUploadsPerUser())
                throw new ImException(429, "QUOTA_EXCEEDED", "未发送附件过多，请先发送或取消");
            String key = "im/" + db.tenant() + "/" + UUID.randomUUID();
            long upload = db.insert(
                    """
                            INSERT INTO im_upload(tenant_id,client_upload_id,payload_hash,conversation_id,uploader_id,object_key,name,declared_size,mime,sha256,expires_at,signed_until)
                            VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                            """,
                    db.tenant(), client, hash, conversation, user, key, request.name(), request.size(), request.mime(),
                    request.sha256(), Timestamp.from(Instant.now().plus(Duration.ofHours(24))), now());
            db.update("UPDATE im_storage_budget SET reserved_bytes=reserved_bytes+? WHERE tenant_id=?", request.size(),
                    db.tenant());
            return sign(db.one("SELECT * FROM im_upload WHERE tenant_id=? AND id=?", db.tenant(), upload));
        });
    }

    public UploadReservation renew(long upload) {
        long user = access.actor();
        access.require(user, "im:file:upload");
        inspector.requireScanner();
        return tx.write(() -> {
            var row = owned(upload, user, true);
            access.writable(db.conversation(n(row, "conversation_id"), false));
            requireInitiated(row);
            return sign(row);
        });
    }

    public UploadStatus complete(long upload) {
        long user = access.actor();
        access.require(user, "im:file:upload");
        return tx.write(() -> {
            var row = owned(upload, user, true);
            if (!"INITIATED".equals(s(row, "state")))
                return status(row);
            access.writable(db.conversation(n(row, "conversation_id"), false));
            requireInitiated(row);
            var head = storage.head(s(row, "object_key"));
            if (head.size() != i(row, "declared_size"))
                throw ImException.conflict("UPLOAD_SIZE_MISMATCH", "已上传文件大小与预约不一致");
            db.update(
                    "UPDATE im_upload SET state='VERIFYING',object_version_id=?,next_attempt_at=CURRENT_TIMESTAMP(6) WHERE tenant_id=? AND id=?",
                    head.version(), db.tenant(), upload);
            return new UploadStatus(Long.toString(upload), "VERIFYING", null, null);
        });
    }

    public UploadStatus get(long upload) {
        long user = access.actor();
        return status(owned(upload, user, false));
    }

    public UploadStatus cancel(long upload) {
        long user = access.actor();
        return tx.write(() -> {
            var row = owned(upload, user, true);
            var file = fileForUpload(upload);
            if (file != null && file.get("message_id") != null)
                throw ImException.conflict("FILE_ALREADY_USED", "已发送附件不能取消");
            if ("EXPIRED".equals(s(row, "state")) || "CANCELLED".equals(s(row, "state")))
                return status(row);
            db.update(
                    "UPDATE im_upload SET state='CANCELLED',lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=?",
                    db.tenant(), upload);
            db.update("UPDATE im_file SET state='CANCELLED' WHERE tenant_id=? AND upload_id=? AND message_id IS NULL",
                    db.tenant(), upload);
            return new UploadStatus(Long.toString(upload), "CANCELLED", null, null);
        });
    }

    private Map<String, Object> owned(long upload, long user, boolean lock) {
        var row = db.one("SELECT * FROM im_upload WHERE tenant_id=? AND id=? AND uploader_id=?", db.tenant(), upload,
                user);
        if (row == null)
            throw ImException.missing();
        access.conversation(n(row, "conversation_id"), user, lock);
        if (lock)
            row = db.one("SELECT * FROM im_upload WHERE tenant_id=? AND id=? AND uploader_id=? FOR UPDATE", db.tenant(),
                    upload, user);
        return row;
    }

    private void requireInitiated(Map<String, Object> row) {
        if (!instant(row, "expires_at").isAfter(Instant.now()))
            throw new ImException(410, "UPLOAD_EXPIRED", "上传预约已过期");
        if (!"INITIATED".equals(s(row, "state")))
            throw ImException.conflict("UPLOAD_ALREADY_COMPLETED", "预约已提交或取消，请查询原上传状态");
    }

    private UploadReservation sign(Map<String, Object> row) {
        Instant expires = Instant.now().plusSeconds(300);
        if (instant(row, "expires_at").isBefore(expires))
            expires = instant(row, "expires_at");
        var signed = storage.sign(s(row, "object_key"), i(row, "declared_size"), s(row, "mime"), s(row, "sha256"),
                expires);
        expires = signed.expiresAt();
        db.update("UPDATE im_upload SET signed_until=GREATEST(signed_until,?) WHERE tenant_id=? AND id=?",
                Timestamp.from(expires), db.tenant(), n(row, "id"));
        return new UploadReservation(id(row, "id"), "PUT", signed.url(), signed.headers(), expires.toString(),
                time(row, "expires_at"), properties.fileLimit());
    }

    private Map<String, Object> fileForUpload(long upload) {
        return db.one("SELECT * FROM im_file WHERE tenant_id=? AND upload_id=?", db.tenant(), upload);
    }

    private UploadStatus status(Map<String, Object> row) {
        var file = fileForUpload(n(row, "id"));
        String state = s(row, "state");
        if ((file == null || file.get("message_id") == null) && !instant(row, "expires_at").isAfter(Instant.now())
                && !"CANCELLED".equals(state))
            state = "EXPIRED";
        return new UploadStatus(id(row, "id"), state, "READY".equals(state) && file != null ? views.file(file) : null,
                s(row, "rejection_reason"));
    }

    public Download download(long fileId, String range) {
        long user = access.actor();
        var file = db.one(
                "SELECT * FROM im_file WHERE tenant_id=? AND id=? AND state='READY' AND message_id IS NOT NULL",
                db.tenant(), fileId);
        if (file == null)
            throw ImException.missing();
        var member = access.member(n(file, "conversation_id"), user);
        var message = db.one("SELECT seq FROM im_message WHERE tenant_id=? AND id=?", db.tenant(),
                n(file, "message_id"));
        if (message == null || n(message, "seq") < n(member, "visible_from_seq"))
            throw ImException.missing();
        int size = i(file, "size");
        long start = 0, end = size - 1;
        if (range != null) {
            try {
                var ranges = HttpRange.parseRanges(range);
                if (ranges.size() != 1)
                    throw new IllegalArgumentException();
                start = ranges.getFirst().getRangeStart(size);
                end = ranges.getFirst().getRangeEnd(size);
                if (start < 0 || start >= size || end < start || end >= size)
                    throw new IllegalArgumentException();
            }
            catch (IllegalArgumentException error) {
                throw new InvalidRange(size);
            }
        }
        var stream = storage.open(s(file, "object_key"), s(file, "object_version_id"),
                range == null ? null : "bytes=" + start + "-" + end);
        return new Download(views.file(file), stream, start, end, range != null);
    }

    public void verifyOne() {
        String owner = UUID.randomUUID().toString();
        var row = tx.write(() -> {
            var candidate = db.one(
                    """
                            SELECT * FROM im_upload WHERE tenant_id=? AND state='VERIFYING' AND next_attempt_at<=CURRENT_TIMESTAMP(6)
                            AND (lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP(6)) ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED
                            """,
                    db.tenant());
            if (candidate != null)
                db.update(
                        "UPDATE im_upload SET lease_owner=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE) WHERE tenant_id=? AND id=?",
                        owner, db.tenant(), n(candidate, "id"));
            return candidate;
        });
        if (row == null)
            return;
        String mime = null, reason = null;
        try (var source = storage.open(s(row, "object_key"), s(row, "object_version_id"), null)) {
            mime = inspector.inspect(source, s(row, "name"), i(row, "declared_size"), s(row, "sha256"));
        }
        catch (ImException error) {
            if (error.status() == 415)
                reason = error.error();
            else {
                defer(row, owner);
                return;
            }
        }
        catch (Exception error) {
            defer(row, owner);
            return;
        }
        String verified = mime, rejected = reason;
        tx.write(() -> {
            long conversation = n(row, "conversation_id"), upload = n(row, "id");
            db.conversation(conversation, true);
            var current = db.one("SELECT * FROM im_upload WHERE tenant_id=? AND id=? FOR UPDATE", db.tenant(), upload);
            if (!"VERIFYING".equals(s(current, "state")) || !owner.equals(s(current, "lease_owner")))
                return null;
            String failure = rejected;
            if (!instant(current, "expires_at").isAfter(Instant.now()))
                failure = "UPLOAD_EXPIRED";
            if (db.member(conversation, n(row, "uploader_id")) == null
                    || !access.can(n(row, "uploader_id"), "im:file:upload"))
                failure = "ACCESS_CHANGED";
            if (failure != null) {
                db.update(
                        "UPDATE im_upload SET state='REJECTED',rejection_reason=?,lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=?",
                        failure, db.tenant(), upload);
            }
            else {
                db.update(
                        """
                                INSERT INTO im_file(tenant_id,upload_id,conversation_id,uploader_id,object_key,object_version_id,name,size,mime,sha256)
                                VALUES(?,?,?,?,?,?,?,?,?,?)
                                """,
                        db.tenant(), upload, conversation, n(row, "uploader_id"), s(row, "object_key"),
                        s(row, "object_version_id"), s(row, "name"), i(row, "declared_size"), verified,
                        s(row, "sha256"));
                db.update(
                        "UPDATE im_upload SET state='READY',lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=?",
                        db.tenant(), upload);
            }
            return null;
        });
    }

    private void defer(Map<String, Object> row, String owner) {
        db.update(
                "UPDATE im_upload SET next_attempt_at=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 1 MINUTE),lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=? AND lease_owner=? AND state='VERIFYING'",
                db.tenant(), n(row, "id"), owner);
    }

    public void cleanupOne() {
        String owner = UUID.randomUUID().toString();
        var candidate = db.one(
                """
                        SELECT u.id,u.conversation_id FROM im_upload u LEFT JOIN im_file f ON f.upload_id=u.id AND f.tenant_id=u.tenant_id
                        WHERE u.tenant_id=? AND u.signed_until<CURRENT_TIMESTAMP(6) AND (u.cleaned=0 OR (u.cleaned=2 AND u.lease_until<CURRENT_TIMESTAMP(6)))
                          AND (u.state IN ('CANCELLED','REJECTED','EXPIRED') OR u.expires_at<CURRENT_TIMESTAMP(6) OR f.message_id IS NOT NULL)
                          AND (u.state<>'VERIFYING' OR u.lease_until IS NULL OR u.lease_until<CURRENT_TIMESTAMP(6)) ORDER BY u.id LIMIT 1
                        """,
                db.tenant());
        if (candidate == null)
            return;
        var row = tx.write(() -> {
            db.conversation(n(candidate, "conversation_id"), true);
            var current = db.one("SELECT * FROM im_upload WHERE tenant_id=? AND id=? FOR UPDATE", db.tenant(),
                    n(candidate, "id"));
            if (n(current, "cleaned") == 1
                    || (n(current, "cleaned") == 2 && instant(current, "lease_until").isAfter(Instant.now())))
                return null;
            // 候选读取与取锁之间可能续签/提交上传；取锁后必须重新检查，不能删除刚续签的对象。
            if (instant(current, "signed_until").isAfter(Instant.now()))
                return null;
            var file = fileForUpload(n(current, "id"));
            boolean bound = file != null && file.get("message_id") != null;
            boolean terminal = Set.of("CANCELLED", "REJECTED", "EXPIRED").contains(s(current, "state"));
            if (!bound && !terminal && instant(current, "expires_at").isAfter(Instant.now()))
                return null;
            if ("VERIFYING".equals(s(current, "state")) && current.get("lease_until") != null
                    && instant(current, "lease_until").isAfter(Instant.now()))
                return null;
            if (!bound) {
                if (!"CANCELLED".equals(s(current, "state")) && !"REJECTED".equals(s(current, "state")))
                    db.update("UPDATE im_upload SET state='EXPIRED' WHERE tenant_id=? AND id=?", db.tenant(),
                            n(current, "id"));
                db.update("UPDATE im_file SET state='EXPIRED' WHERE tenant_id=? AND upload_id=? AND message_id IS NULL",
                        db.tenant(), n(current, "id"));
            }
            db.update(
                    "UPDATE im_upload SET cleaned=2,lease_owner=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE) WHERE tenant_id=? AND id=?",
                    owner, db.tenant(), n(current, "id"));
            current.put("keep_version", bound ? s(file, "object_version_id") : null);
            return current;
        });
        if (row == null)
            return;
        storage.cleanup(s(row, "object_key"), s(row, "keep_version"));
        tx.write(() -> {
            db.conversation(n(row, "conversation_id"), true);
            var current = db.one("SELECT * FROM im_upload WHERE tenant_id=? AND id=? FOR UPDATE", db.tenant(),
                    n(row, "id"));
            if (n(current, "cleaned") != 2 || !owner.equals(s(current, "lease_owner")))
                return null;
            db.update("UPDATE im_upload SET cleaned=1,lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=?",
                    db.tenant(), n(row, "id"));
            if (s(row, "keep_version") == null)
                db.update("UPDATE im_storage_budget SET reserved_bytes=GREATEST(0,reserved_bytes-?) WHERE tenant_id=?",
                        i(row, "declared_size"), db.tenant());
            return null;
        });
    }

}

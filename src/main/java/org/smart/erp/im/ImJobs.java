package org.smart.erp.im;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smart.erp.im.attachment.ImAttachments;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.TenantExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import static org.smart.erp.im.ImStore.*;

@Component
public class ImJobs {

    private static final Logger log = LoggerFactory.getLogger(ImJobs.class);

    private final ImProperties properties;

    private final TenantProperties tenancy;

    private final TenantExecutor tenants;

    private final ImTransactions tx;

    private final ImStore db;

    private final ImRedis redis;

    private final ImJson json;

    private final ImAttachments files;

    public ImJobs(ImProperties properties, TenantProperties tenancy, TenantExecutor tenants, ImTransactions tx,
            ImStore db, ImRedis redis, ImJson json, ImAttachments files) {
        this.properties = properties;
        this.tenancy = tenancy;
        this.tenants = tenants;
        this.tx = tx;
        this.db = db;
        this.redis = redis;
        this.json = json;
        this.files = files;
    }

    private void run(Runnable action) {
        // 当前 multi 业务门禁未开放，后台任务同样不能绕过它；单企业任务仍显式绑定身份。
        if (!properties.isEnabled() || tenancy.multi())
            return;
        try {
            tenants.forTenant(tenancy.getSingleTenantId()).run(action);
        }
        catch (RuntimeException e) {
            log.warn("IM background task failed: {}", e.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelayString = "${im.outbox-delay-ms:100}")
    public void dispatch() {
        run(this::dispatchBatch);
    }

    public void dispatchBatch() {
        String owner = UUID.randomUUID().toString();
        var rows = tx.write(() -> {
            var batch = db.rows(
                    """
                            SELECT * FROM im_outbox WHERE tenant_id=? AND
                            ((state='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP(6)) OR (state='PROCESSING' AND lease_until<CURRENT_TIMESTAMP(6)))
                            ORDER BY id LIMIT 1000 FOR UPDATE SKIP LOCKED
                            """,
                    db.tenant());
            for (var row : batch)
                db.update(
                        "UPDATE im_outbox SET state='PROCESSING',attempts=attempts+1,lease_owner=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 30 SECOND) WHERE tenant_id=? AND id=?",
                        owner, db.tenant(), n(row, "id"));
            return batch;
        });
        if (rows.isEmpty())
            return;
        try {
            for (long user : rows.stream().map(r -> n(r, "recipient_user_id")).distinct().toList())
                redis.publish(json.write(new ImConfiguration.Wakeup(db.tenant(), user)));
            db.update(
                    "UPDATE im_outbox SET state='DONE',lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND lease_owner=? AND state='PROCESSING'",
                    db.tenant(), owner);
        }
        catch (RuntimeException error) {
            db.update(
                    """
                            UPDATE im_outbox SET state=IF(attempts>=10,'FAILED','PENDING'),next_attempt_at=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 SECOND),
                            lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND lease_owner=? AND state='PROCESSING'
                            """,
                    db.tenant(), owner);
            log.warn("IM outbox publish failed; persisted events remain available for REST sync");
        }
    }

    @Scheduled(fixedDelayString = "${im.verify-delay-ms:1000}")
    public void verify() {
        if (properties.getStorage().isEnabled())
            run(files::verifyOne);
    }

    @Scheduled(fixedDelayString = "${im.file-cleanup-delay-ms:1000}")
    public void cleanupFiles() {
        if (properties.getStorage().isEnabled())
            run(files::cleanupOne);
    }

    @Scheduled(fixedDelayString = "${im.retention-delay-ms:3600000}", initialDelay = 60000)
    public void retention() {
        run(this::prune);
    }

    public void prune() {
        tx.write(() -> {
            db.update(
                    """
                            UPDATE im_user_stream s JOIN (SELECT user_id,MAX(event_seq) seq FROM im_user_event
                            WHERE tenant_id=? AND created_at<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 7 DAY) GROUP BY user_id) x ON x.user_id=s.user_id
                            SET s.pruned_through_seq=GREATEST(s.pruned_through_seq,x.seq) WHERE s.tenant_id=?
                            """,
                    db.tenant(), db.tenant());
            db.update(
                    "DELETE FROM im_user_event WHERE tenant_id=? AND created_at<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 7 DAY) LIMIT 10000",
                    db.tenant());
            db.update(
                    "DELETE FROM im_outbox WHERE tenant_id=? AND created_at<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 7 DAY) LIMIT 10000",
                    db.tenant());
            db.update(
                    """
                            DELETE FROM im_event WHERE tenant_id=? AND created_at<DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 7 DAY)
                            AND NOT EXISTS (SELECT 1 FROM im_user_event u WHERE u.tenant_id=im_event.tenant_id AND u.event_id=im_event.id) LIMIT 10000
                            """,
                    db.tenant());
            return null;
        });
    }

}

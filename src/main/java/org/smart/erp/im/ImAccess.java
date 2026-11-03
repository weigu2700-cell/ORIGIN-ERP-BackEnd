package org.smart.erp.im;

import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.tenant.config.TenantProperties;
import org.springframework.stereotype.Component;
import static org.smart.erp.im.ImStore.*;

@Component
public class ImAccess {

    private final ImStore db;

    private final CurrentUser current;

    private final ImProperties properties;

    private final TenantProperties tenancy;

    public ImAccess(ImStore db, CurrentUser current, ImProperties properties, TenantProperties tenancy) {
        this.db = db;
        this.current = current;
        this.properties = properties;
        this.tenancy = tenancy;
    }

    public void enabled() {
        if (!properties.isEnabled() || tenancy.multi())
            throw new ImException(503, "CHAT_UNAVAILABLE", "聊天尚未开放");
    }

    public long actor() {
        enabled();
        long user = current.getUserId();
        require(user, "im:access");
        return user;
    }

    public void require(long user, String code) {
        enabled();
        if (!can(user, code))
            throw new ImException(403, "IM_NOT_ENABLED", "账号不可用或没有聊天权限");
    }

    public boolean can(long user, String code) {
        return db.one(
                """
                        SELECT u.id FROM sys_user u WHERE u.id=? AND u.status=1 AND u.deleted=0 AND EXISTS (
                          SELECT 1 FROM sys_permission p WHERE p.code=? AND p.status=1 AND p.deleted=0 AND EXISTS (
                            SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
                            WHERE ur.user_id=u.id AND r.status=1 AND r.deleted=0 AND
                              (r.code='admin' OR EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id))
                          )
                        )
                        """,
                user, code) != null;
    }

    public java.util.Map<String, Object> member(long conversation, long user) {
        var member = db.member(conversation, user);
        if (member == null)
            throw ImException.missing();
        return member;
    }

    public java.util.Map<String, Object> conversation(long id, long user, boolean lock) {
        var row = db.conversation(id, lock);
        if (row == null)
            throw ImException.missing();
        member(id, user);
        return row;
    }

    public void writable(java.util.Map<String, Object> conversation) {
        if (!"ACTIVE".equals(s(conversation, "status")))
            throw ImException.conflict("GROUP_NOT_WRITABLE", "群已解散，只能查看历史");
    }

}

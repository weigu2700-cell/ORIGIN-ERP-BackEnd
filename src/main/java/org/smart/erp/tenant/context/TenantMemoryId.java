package org.smart.erp.tenant.context;

import org.smart.erp.tenant.TenantException;

/** 服务端生成；不接受客户端自选 memory ID。 */
public record TenantMemoryId(String tenantId, long userId, long conversationId) {
    public TenantMemoryId {
        if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,64}") || userId <= 0 || conversationId <= 0)
            throw new TenantException(403, "无效的模型记忆身份");
    }
    public String encode() { return "t:" + tenantId + ":u:" + userId + ":c:" + conversationId; }
    public static TenantMemoryId parse(String value) {
        if (value == null || !value.matches("t:[A-Za-z0-9_-]{1,64}:u:[1-9][0-9]{0,18}:c:[1-9][0-9]{0,18}"))
            throw new TenantException(403, "模型记忆缺少企业/用户身份");
        var parts = value.split(":");
        try { return new TenantMemoryId(parts[1], Long.parseLong(parts[3]), Long.parseLong(parts[5])); }
        catch (NumberFormatException error) { throw new TenantException(403, "无效的模型记忆身份"); }
    }
}

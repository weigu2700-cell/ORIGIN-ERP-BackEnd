package org.smart.erp.tenant.context;

import org.smart.erp.tenant.TenantException;

/**
 * 模型记忆的唯一标识。
 *
 * <p>由服务端生成，格式为 {@code t:{tenantId}:u:{userId}:c:{conversationId}}，
 * 不接受客户端自行指定。</p>
 */
public record TenantMemoryId(String tenantId, long userId, long conversationId) {

    private static final String TENANT_ID_PATTERN = "[A-Za-z0-9_-]{1,64}";
    private static final String ENCODED_PATTERN =
            "t:[A-Za-z0-9_-]{1,64}:u:[1-9][0-9]{0,18}:c:[1-9][0-9]{0,18}";

    public TenantMemoryId {
        if (tenantId == null
                || !tenantId.matches(TENANT_ID_PATTERN)
                || userId <= 0
                || conversationId <= 0) {
            throw new TenantException(403, "无效的模型记忆身份");
        }
    }

    public String encode() {
        return "t:" + tenantId + ":u:" + userId + ":c:" + conversationId;
    }

    public static TenantMemoryId parse(String value) {
        if (value == null || !value.matches(ENCODED_PATTERN)) {
            throw new TenantException(403, "模型记忆缺少企业/用户身份");
        }

        String[] parts = value.split(":");
        try {
            return new TenantMemoryId(
                    parts[1],
                    Long.parseLong(parts[3]),
                    Long.parseLong(parts[5])
            );
        } catch (NumberFormatException error) {
            throw new TenantException(403, "无效的模型记忆身份");
        }
    }
}

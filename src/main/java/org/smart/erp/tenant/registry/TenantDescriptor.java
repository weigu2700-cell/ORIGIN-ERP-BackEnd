package org.smart.erp.tenant.registry;

import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.migration.TenantFlyway;

/**
 * 企业登记信息与数据库状态快照。
 *
 * @param id             企业 ID
 * @param code           企业编码
 * @param name           企业名称
 * @param connectionRef  服务端连接配置引用
 * @param routingVersion 路由版本，用于检测连接配置是否变更
 * @param status         企业状态
 * @param schemaStatus   企业库结构状态
 * @param schemaVersion  企业库结构版本
 */
public record TenantDescriptor(
        String id,
        String code,
        String name,
        String connectionRef,
        long routingVersion,
        String status,
        String schemaStatus,
        String schemaVersion
) {

    /** 校验企业可用且数据库就绪，返回自身便于链式调用。 */
    public TenantDescriptor requireActive() {
        if (!"ACTIVE".equals(status)) {
            throw new TenantException(403, "企业尚未开通或已停用");
        }
        if (!"READY".equals(schemaStatus)) {
            throw new TenantException(503, "企业数据库尚未就绪");
        }
        if (!TenantFlyway.TENANT_VERSION.equals(schemaVersion)) {
            throw new TenantException(503, "企业数据库版本与当前服务不兼容");
        }
        return this;
    }
}

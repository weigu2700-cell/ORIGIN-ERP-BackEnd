package org.smart.erp.tenant.datasource;

import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.registry.TenantCatalog;
import org.springframework.jdbc.datasource.AbstractDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** 无默认目标，无上下文时不能取得连接，也不接受调用者传入数据库凭据。 */
public final class TenantRoutingDataSource extends AbstractDataSource {
    private final TenantCatalog catalog;
    private final TenantDataSourceRegistry registry;
    public TenantRoutingDataSource(TenantCatalog catalog, TenantDataSourceRegistry registry) { this.catalog = catalog; this.registry = registry; }
    @Override public Connection getConnection() {
        var tenant = catalog.byId(TenantScope.required());
        try { return registry.get(tenant).getConnection(); }
        catch (SQLException error) { throw new TenantException(503, "企业数据库暂时不可用"); }
    }
    @Override public Connection getConnection(String username, String password) {
        throw new TenantException(403, "不接受请求级数据库凭据");
    }
}

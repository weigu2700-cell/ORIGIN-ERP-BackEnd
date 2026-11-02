package org.smart.erp.tenant.registry;

import org.smart.erp.tenant.TenantException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import javax.sql.DataSource;

/** 独立平台数据源，不通过业务 Mapper 或租户路由读取。 */
public final class JdbcTenantCatalog implements TenantCatalog {
    private final JdbcTemplate jdbc;
    public JdbcTenantCatalog(DataSource platform) { jdbc = new JdbcTemplate(platform); }
    private TenantDescriptor lookup(String column, String value) {
        try {
            var rows = jdbc.query("SELECT t.id,t.code,t.name,t.status,d.connection_ref,d.routing_version,d.schema_status,d.schema_version " +
                    "FROM plt_tenant t JOIN plt_tenant_database d ON t.id=d.tenant_id WHERE t." + column + "=?",
                    (r,n) -> new TenantDescriptor(r.getString("id"), r.getString("code"), r.getString("name"),
                            r.getString("connection_ref"), r.getLong("routing_version"), r.getString("status"), r.getString("schema_status"), r.getString("schema_version")), value);
            if (rows.size() != 1) throw new TenantException(403, "企业不可用");
            return rows.getFirst().requireActive();
        } catch (DataAccessException error) { throw new TenantException(503, "企业登记服务暂时不可用"); }
    }
    @Override public TenantDescriptor byId(String id) { return lookup("id", id); }
    @Override public TenantDescriptor byCode(String code) { return lookup("code", code); }
}

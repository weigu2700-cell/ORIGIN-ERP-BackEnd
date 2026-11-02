package org.smart.erp.tenant.registry;

import org.smart.erp.tenant.TenantException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/** 基于独立平台数据源的企业目录，不经过业务 Mapper 或租户路由。 */
public final class JdbcTenantCatalog implements TenantCatalog {

    private static final String LOOKUP_SQL = """
            SELECT t.id, t.code, t.name, t.status,
                   d.connection_ref, d.routing_version, d.schema_status, d.schema_version
            FROM plt_tenant t
            JOIN plt_tenant_database d ON t.id = d.tenant_id
            WHERE t.%s = ?
            """;

    private final JdbcTemplate jdbc;

    public JdbcTenantCatalog(DataSource platform) {
        jdbc = new JdbcTemplate(platform);
    }

    private TenantDescriptor lookup(String column, String value) {
        try {
            List<TenantDescriptor> rows = jdbc.query(
                    LOOKUP_SQL.formatted(column),
                    (row, rowNum) -> new TenantDescriptor(
                            row.getString("id"),
                            row.getString("code"),
                            row.getString("name"),
                            row.getString("connection_ref"),
                            row.getLong("routing_version"),
                            row.getString("status"),
                            row.getString("schema_status"),
                            row.getString("schema_version")
                    ),
                    value
            );
            if (rows.size() != 1) {
                throw new TenantException(403, "企业不可用");
            }
            return rows.getFirst().requireActive();
        } catch (DataAccessException error) {
            throw new TenantException(503, "企业登记服务暂时不可用");
        }
    }

    @Override
    public TenantDescriptor byId(String id) {
        return lookup("id", id);
    }

    @Override
    public TenantDescriptor byCode(String code) {
        return lookup("code", code);
    }
}

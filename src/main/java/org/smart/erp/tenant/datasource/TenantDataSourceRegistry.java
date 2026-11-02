package org.smart.erp.tenant.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.smart.erp.tenant.TenantException;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.registry.TenantDescriptor;
import java.util.HashMap;
import java.util.Map;

/** T2：受容量限制的按需连接池；回收与滚动配置更新后续完善。 */
public final class TenantDataSourceRegistry implements AutoCloseable {
    private record Pool(long version, HikariDataSource dataSource) {}
    private final Map<String, Pool> pools = new HashMap<>();
    private final TenantProperties properties;
    private boolean closed;
    public TenantDataSourceRegistry(TenantProperties properties) {
        this.properties = properties;
        if (properties.getMaxPools() < 1 || properties.getPoolSize() < 1)
            throw new IllegalArgumentException("连接池容量必须为正数");
        var targets = new java.util.HashSet<String>();
        if (properties.getPlatform().getUrl() != null) targets.add(target(properties.getPlatform().getUrl()));
        properties.getConnections().values().forEach(connection -> {
            if (!targets.add(target(connection.getUrl()))) throw new IllegalArgumentException("平台库与各租户连接必须指向不同数据库");
        });
    }
    private static String target(String url) {
        if (url == null || !url.startsWith("jdbc:mysql://")) throw new IllegalArgumentException("需要标准 MySQL JDBC 地址");
        var uri = java.net.URI.create(url.substring(5));
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getPath() == null || uri.getPath().length() < 2)
            throw new IllegalArgumentException("数据库地址必须明确主机与库名，凭据使用独立配置");
        return uri.getHost().toLowerCase(java.util.Locale.ROOT) + ":" + (uri.getPort() < 0 ? 3306 : uri.getPort()) + uri.getPath();
    }
    public synchronized HikariDataSource get(TenantDescriptor tenant) {
        if (closed) throw new TenantException(503, "企业连接服务已关闭");
        tenant.requireActive();
        Pool existing = pools.get(tenant.id());
        if (existing != null) {
            // 不在活跃请求中关闭或替换连接池；配置变更要求受控重启。
            if (existing.version() != tenant.routingVersion()) throw new TenantException(503, "企业连接配置已更新，请联系运维重载服务");
            return existing.dataSource();
        }
        if (pools.size() >= properties.getMaxPools()) throw new TenantException(503, "企业连接容量已满");
        var connection = properties.getConnections().get(tenant.connectionRef());
        if (connection == null) throw new TenantException(503, "企业连接尚未配置");
        var pool = create(connection, "tenant-" + tenant.id(), properties.getPoolSize());
        pools.put(tenant.id(), new Pool(tenant.routingVersion(), pool));
        return pool;
    }
    public static HikariDataSource create(TenantProperties.Connection connection, String name, int size) {
        if (size < 1 || connection.getUrl() == null || !connection.getUrl().startsWith("jdbc:mysql:"))
            throw new IllegalArgumentException("需要有效的 MySQL 连接配置与连接池上限");
        var pool = new HikariDataSource();
        pool.setJdbcUrl(connection.getUrl()); pool.setUsername(connection.getUsername()); pool.setPassword(connection.getPassword());
        pool.setPoolName(name); pool.setMaximumPoolSize(size); pool.setMinimumIdle(0);
        pool.setConnectionTimeout(3000); pool.setInitializationFailTimeout(-1);
        return pool;
    }
    @Override public synchronized void close() { pools.values().forEach(p -> p.dataSource().close()); pools.clear(); closed = true; }
}

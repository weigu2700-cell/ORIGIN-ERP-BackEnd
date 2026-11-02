package org.smart.erp.tenant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/** 多租户运行模式与数据源连接配置。 */
@Getter
@Setter
@ConfigurationProperties("tenancy")
public class TenantProperties {

    /** 运行模式：单企业或多企业。 */
    public enum Mode {
        SINGLE,
        MULTI
    }

    /** 运行模式，默认单企业。 */
    private Mode mode = Mode.SINGLE;

    /** 单企业模式下的企业 ID。 */
    private String singleTenantId = "single";

    /** 单企业模式下的企业编码。 */
    private String singleTenantCode = "single";

    /** multi 模式下是否兼容未携带企业信息的旧令牌。 */
    private boolean allowLegacyTokens = true;

    /** 业务租户连接池的最大数量。 */
    private int maxPools = 16;

    /** 单个租户连接池的容量。 */
    private int poolSize = 4;

    /** 平台库连接配置。 */
    private Connection platform = new Connection();

    /** 连接引用与实际连接配置的映射。 */
    private Map<String, Connection> connections = new HashMap<>();

    public boolean multi() {
        return mode == Mode.MULTI;
    }

    /** 数据源连接配置。 */
    @Getter
    @Setter
    public static class Connection {

        private String url;
        private String username;
        private String password;
    }
}

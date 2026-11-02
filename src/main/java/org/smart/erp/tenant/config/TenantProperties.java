package org.smart.erp.tenant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

@Getter @Setter
@ConfigurationProperties("tenancy")
public class TenantProperties {
    public enum Mode { SINGLE, MULTI }
    private Mode mode = Mode.SINGLE;
    private String singleTenantId = "single";
    private String singleTenantCode = "single";
    private boolean allowLegacyTokens = true;
    private int maxPools = 16;
    private int poolSize = 4;
    private Connection platform = new Connection();
    private Map<String, Connection> connections = new HashMap<>();
    public boolean multi() { return mode == Mode.MULTI; }
    @Getter @Setter
    public static class Connection {
        private String url;
        private String username;
        private String password;
    }
}

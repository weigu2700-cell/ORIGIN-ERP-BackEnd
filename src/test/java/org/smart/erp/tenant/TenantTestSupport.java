package org.smart.erp.tenant;

import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.*;
import org.smart.erp.tenant.registry.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

public final class TenantTestSupport {
    private TenantTestSupport() {}
    public static TenantKeyspace singleKeys() { return new TenantKeyspace(new TenantProperties()); }
    public static TenantExecutor singleExecutor() {
        var properties = new TenantProperties();
        return new TenantExecutor(new TenantKeyspace(properties), new TenantResolver(properties,
                new StaticListableBeanFactory().getBeanProvider(TenantCatalog.class)));
    }
    public static TenantResolver multiResolver(TenantCatalog catalog) {
        var properties = new TenantProperties(); properties.setMode(TenantProperties.Mode.MULTI);
        var beans = new StaticListableBeanFactory(java.util.Map.of("catalog",catalog));
        return new TenantResolver(properties, beans.getBeanProvider(TenantCatalog.class));
    }
    public static TenantKeyspace multiKeys() {
        var properties = new TenantProperties(); properties.setMode(TenantProperties.Mode.MULTI);
        return new TenantKeyspace(properties);
    }
}

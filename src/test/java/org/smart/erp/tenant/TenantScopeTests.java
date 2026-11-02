package org.smart.erp.tenant;

import org.junit.jupiter.api.Test;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.registry.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

class TenantScopeTests {
    @Test void nestedScopeRestoresAndExceptionsDoNotLeak() {
        try(var outer=TenantScope.open("a")) {
            try(var inner=TenantScope.open("a")) { assertThat(TenantScope.required()).isEqualTo("a"); }
            assertThatThrownBy(()->TenantScope.open("b")).isInstanceOf(TenantException.class);
            assertThat(TenantScope.required()).isEqualTo("a");
        }
        assertThat(TenantScope.current()).isNull();
        assertThatThrownBy(TenantScope::required).isInstanceOf(TenantException.class);
    }
    @Test void threadPoolReuseDoesNotInheritTenant() throws Exception {
        try(var executor=Executors.newSingleThreadExecutor()) {
            executor.submit(()-> {try(var scope=TenantScope.open("a")){throw new IllegalStateException("expected");}});
            assertThat(executor.submit(TenantScope::current).get()).isNull();
        }
    }
    @Test void tenantMustBeEstablishedBeforeTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try { assertThatThrownBy(()->TenantScope.open("a")).isInstanceOf(TenantException.class); }
        finally { TransactionSynchronizationManager.clear(); }
    }
    @Test void singleModeAcceptsLegacyOnlyWhenConfiguredAndRejectsAnotherTenant() {
        var properties=new TenantProperties();
        var resolver=new TenantResolver(properties,new StaticListableBeanFactory().getBeanProvider(TenantCatalog.class));
        assertThat(resolver.authenticated(null).id()).isEqualTo("single");
        assertThatThrownBy(()->resolver.authenticated("b")).isInstanceOf(TenantException.class);
        properties.setAllowLegacyTokens(false);
        assertThatThrownBy(()->resolver.authenticated(null)).isInstanceOf(TenantException.class);
    }
    @Test void rejectsDuplicateDatabaseTargetsBeforeServingRequests() {
        var properties = new TenantProperties();
        var first = new TenantProperties.Connection(); first.setUrl("jdbc:mysql://localhost/tenant_a?useSSL=false");
        var second = new TenantProperties.Connection(); second.setUrl("jdbc:mysql://localhost:3306/tenant_a?characterEncoding=utf8");
        properties.getConnections().put("a", first); properties.getConnections().put("b", second);
        assertThatThrownBy(() -> new org.smart.erp.tenant.datasource.TenantDataSourceRegistry(properties))
                .isInstanceOf(IllegalArgumentException.class);
    }

}

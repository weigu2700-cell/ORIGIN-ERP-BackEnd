package org.smart.erp.tenant;

import org.junit.jupiter.api.*;
import org.smart.erp.tenant.context.*;
import org.smart.erp.tenant.registry.*;
import org.smart.erp.common.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantContextTests {
    private TenantCatalog catalog;
    private TenantExecutor executor;
    static TenantDescriptor descriptor(String id) { return new TenantDescriptor(id,id,id,id,1,"ACTIVE","READY",org.smart.erp.tenant.migration.TenantFlyway.TENANT_VERSION); }
    @BeforeEach void setup() {
        catalog = mock(TenantCatalog.class);
        when(catalog.byId(anyString())).thenAnswer(call -> descriptor(call.getArgument(0)));
        executor = new TenantExecutor(TenantTestSupport.multiKeys(), TenantTestSupport.multiResolver(catalog));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    static void login(String id, long user) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new LoginUser(user,"user",id), null, List.of()));
    }
    @Test void missingTenantCannotUseRedisOrCaptureWork() {
        assertThatThrownBy(() -> TenantTestSupport.multiKeys().key("erp:test")).isInstanceOf(TenantException.class);
        assertThatThrownBy(executor::captureTenant).isInstanceOf(TenantException.class);
    }
    @Test void redisKeysCannotBeRemappedToAnotherTenant() {
        var serializer = new TenantRedisKeySerializer(TenantTestSupport.multiKeys());
        try (var scope = TenantScope.open("a")) {
            assertThat(new String(serializer.serialize("key"), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("erp:t:a:key");
            assertThat(serializer.deserialize(serializer.serialize("key"))).isEqualTo("key");
            assertThatThrownBy(() -> serializer.deserialize("erp:t:b:key".getBytes())).isInstanceOf(TenantException.class);
        }
    }
    @Test void snapshotCopiesMutableUserAndRestoresWorkerOnFailureAndReuse() throws Exception {
        TenantExecution snapshot;
        try (var scope = TenantScope.open("a")) {
            login("a", 80001);
            snapshot = executor.capture();
            ((LoginUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).setTenantId("b");
        }
        try (var workers = Executors.newSingleThreadExecutor()) {
            assertThat(workers.submit(() -> snapshot.call(() -> TenantScope.required()+":"+((LoginUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId())).get()).isEqualTo("a:80001");
            workers.submit(() -> assertThatThrownBy(() -> snapshot.run(() -> { throw new IllegalStateException("test"); })).isInstanceOf(IllegalStateException.class)).get();
            assertThat(workers.submit(TenantScope::current).get()).isNull();
            assertThat(workers.submit(() -> SecurityContextHolder.getContext().getAuthentication()).get()).isNull();
        }
    }
    @Test void activeScopeCannotBeReplacedByAnotherSnapshot() {
        TenantExecution snapshot;
        try (var scope = TenantScope.open("a")) { login("a", 7); snapshot=executor.capture(); }
        try (var scope = TenantScope.open("b")) {
            login("b",7); var original=SecurityContextHolder.getContext();
            assertThatThrownBy(() -> snapshot.run(() -> fail("must not execute"))).isInstanceOf(TenantException.class);
            assertThat(TenantScope.current()).isEqualTo("b"); assertThat(SecurityContextHolder.getContext()).isSameAs(original);
        }
    }
    @Test void suspensionAfterCapturePreventsDeferredWork() {
        TenantWork snapshot;
        try (var scope = TenantScope.open("a")) { snapshot=executor.captureTenant(); }
        when(catalog.byId("a")).thenThrow(new TenantException(403,"suspended"));
        var called=new AtomicBoolean();
        assertThatThrownBy(() -> snapshot.run(() -> called.set(true))).isInstanceOf(TenantException.class);
        assertThat(called).isFalse(); assertThat(TenantScope.current()).isNull();
    }
}

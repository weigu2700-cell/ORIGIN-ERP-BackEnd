package org.smart.erp.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smart.erp.common.security.*;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.service.PermissionService;
import org.smart.erp.system.service.UserService;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.registry.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TenantFilterCompatibilityTests {
    private MockMvc mvc;
    private UserService users;
    private JwtUtil jwt;
    @BeforeEach void setup() {
        var properties = new TenantProperties();
        var resolver = new TenantResolver(properties, new StaticListableBeanFactory().getBeanProvider(TenantCatalog.class));
        jwt = new JwtUtil(Base64.getEncoder().encodeToString("single-compatibility-secret-at-least-32-bytes".getBytes()),60000);
        users=mock(UserService.class);
        var user=new User();user.setId(42L);user.setUsername("operator");user.setStatus(UserStatus.NORMAL);
        when(users.detailUserById(42L)).thenReturn(user);
        var permissions=mock(PermissionService.class);
        when(permissions.detailCurrentUserPermission(42L)).thenReturn(List.of());
        mvc=MockMvcBuilders.standaloneSetup(new CompatibilityController()).addFilters(
                new JwtAuthenticationFilter(jwt,users,permissions,resolver,properties,new ObjectMapper())).build();
    }
    @Test void singleModeAcceptsOldTokenAndCleansContext() throws Exception {
        mvc.perform(get("/compatibility").header("Authorization","Bearer "+jwt.generateToken(42L)))
                .andExpect(status().isOk()).andExpect(content().string("single"));
        assertThat(TenantScope.current()).isNull();
    }
    @Test void staleBearerDoesNotBlockPasswordLogin() throws Exception {
        mvc.perform(post("/auth/login").header("Authorization","Bearer expired-invalid"))
                .andExpect(status().isOk()).andExpect(content().string("login"));
        verifyNoInteractions(users);
    }
    @Test void databaseFailureIs503InsteadOfLoggingUserOut() throws Exception {
        when(users.detailUserById(42L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture failure"));
        mvc.perform(get("/compatibility").header("Authorization","Bearer "+jwt.generateToken(42L)))
                .andExpect(status().isServiceUnavailable());
        assertThat(TenantScope.current()).isNull();
    }
    @org.springframework.context.annotation.Profile("tenant-fixture")
    @RestController static class CompatibilityController {
        @GetMapping("/compatibility") String read() { return TenantScope.required(); }
        @PostMapping("/auth/login") String login() { return "login"; }
    }
}

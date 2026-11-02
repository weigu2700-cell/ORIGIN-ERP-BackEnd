package org.smart.erp.tenant.web;

import org.smart.erp.common.result.Result;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.system.service.UserService;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.registry.TenantDescriptor;
import org.smart.erp.tenant.registry.TenantResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 只公开登录配置与当前已认证会话；不枚举企业，也不开放业务门禁。 */
@RestController
@RequestMapping("/auth/tenant")
public class TenantAccessController {

    private final TenantResolver resolver;
    private final TenantProperties properties;
    private final CurrentUser current;
    private final UserService users;

    public TenantAccessController(TenantResolver resolver,
                                  TenantProperties properties,
                                  CurrentUser current,
                                  UserService users) {
        this.resolver = resolver;
        this.properties = properties;
        this.current = current;
        this.users = users;
    }

    public record Configuration(String mode, TenantController.Enterprise organization) {
    }

    public record SessionUser(String id, String username, String realName) {
    }

    public record Session(String tenantId,
                          TenantController.Enterprise organization,
                          boolean businessAvailable,
                          SessionUser user) {
    }

    @GetMapping("/context")
    public Result<Configuration> configuration() {
        TenantDescriptor organization = properties.multi() ? null : resolver.login(null);
        TenantController.Enterprise enterprise = organization == null
                ? null
                : new TenantController.Enterprise(organization.code(), organization.name());
        return Result.success(new Configuration(properties.multi() ? "multi" : "single", enterprise));
    }

    @GetMapping("/session")
    public Result<Session> session() {
        TenantDescriptor tenant = resolver.authenticated(TenantScope.required());
        var user = users.detailUserById(current.getUserId());
        return Result.success(new Session(
                tenant.id(),
                new TenantController.Enterprise(tenant.code(), tenant.name()),
                !properties.multi(),
                new SessionUser(user.getId().toString(), user.getUsername(), user.getRealName())
        ));
    }
}

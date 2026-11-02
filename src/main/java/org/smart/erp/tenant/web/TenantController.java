package org.smart.erp.tenant.web;

import org.smart.erp.common.result.Result;
import org.smart.erp.tenant.registry.TenantResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录前根据企业编码解析企业信息。 */
@RestController
@RequestMapping("/auth/tenant")
public class TenantController {

    private final TenantResolver resolver;

    public TenantController(TenantResolver resolver) {
        this.resolver = resolver;
    }

    public record ResolveRequest(String tenantCode) {
    }

    public record Enterprise(String code, String name) {
    }

    @PostMapping("/resolve")
    public Result<Enterprise> resolve(@RequestBody ResolveRequest request) {
        var tenant = resolver.login(request.tenantCode());
        return Result.success(new Enterprise(tenant.code(), tenant.name()));
    }
}

package org.smart.erp.common.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 全局配置。
 * <p>
 * 访问地址：
 * - Swagger UI:  http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartErpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart ERP Server API")
                        .description("smart-erp-server 接口文档")
                        .version("v1.0.0")
                        .license(new License().name("MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Smart ERP 项目文档")
                        .url("https://github.com/weigu2700-cell/smart-erp"));
    }
}

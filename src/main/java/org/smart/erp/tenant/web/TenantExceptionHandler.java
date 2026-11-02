package org.smart.erp.tenant.web;

import org.smart.erp.common.result.Result;
import org.smart.erp.tenant.TenantException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将租户异常转换为对应的 HTTP 状态码。 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantExceptionHandler {

    @ExceptionHandler(TenantException.class)
    public ResponseEntity<Result<Void>> handle(TenantException error) {
        return ResponseEntity.status(error.status())
                .body(Result.fail(error.status(), error.getMessage()));
    }
}

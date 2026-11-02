package org.smart.erp.tenant.web;

import org.smart.erp.common.result.Result;
import org.smart.erp.tenant.TenantException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class TenantExceptionHandler {
    @ExceptionHandler(TenantException.class)
    public ResponseEntity<Result<Void>> handle(TenantException error) {
        return ResponseEntity.status(error.status()).body(Result.fail(error.status(), error.getMessage()));
    }
}

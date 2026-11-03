package org.smart.erp.im;

import org.slf4j.LoggerFactory;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.result.Result;
import org.smart.erp.im.attachment.ImAttachments;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ImController.class)
public class ImErrors {

    @ExceptionHandler(ImException.class)
    public ResponseEntity<Result<?>> domain(ImException e) {
        var builder = ResponseEntity.status(e.status());
        if (e.status() == 429)
            builder.header("Retry-After", "1");
        if (e instanceof ImAttachments.InvalidRange range)
            builder.header("Content-Range", "bytes */" + range.size);
        return builder.body(error(e.status(), e.error(), e.getMessage()));
    }

    @ExceptionHandler({ ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class })
    public ResponseEntity<Result<?>> invalid(Exception e) {
        return ResponseEntity.badRequest().body(error(400, "INVALID_ARGUMENT", "参数缺失或格式不正确"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> identity(BusinessException e) {
        return ResponseEntity.status(e.getCode()).body(error(e.getCode(), "UNAUTHENTICATED", e.getMessage()));
    }

    @ExceptionHandler({ DataAccessException.class, RedisConnectionFailureException.class })
    public ResponseEntity<Result<?>> unavailable(Exception e) {
        return ResponseEntity.status(503).body(error(503, "CHAT_UNAVAILABLE", "聊天依赖暂时不可用，请保留输入稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> unexpected(Exception e) {
        String requestId = UUID.randomUUID().toString();
        LoggerFactory.getLogger(ImErrors.class).error("IM request failed requestId={}", requestId, e);
        return ResponseEntity.internalServerError()
            .body(new Result<>(500, "聊天请求失败", Map.of("error", "INTERNAL_ERROR", "requestId", requestId)));
    }

    private Result<?> error(int status, String error, String message) {
        return new Result<>(status, message,
                Map.of("error", error, "requestId", UUID.randomUUID().toString(), "details", Map.of()));
    }

}

package org.smart.erp.tenant;

/** 租户上下文异常，携带需要返回给调用方的 HTTP 状态码。 */
public class TenantException extends RuntimeException {

    private final int status;

    public TenantException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}

package org.smart.erp.tenant;

public class TenantException extends RuntimeException {
    private final int status;
    public TenantException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
}

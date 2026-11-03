package org.smart.erp.im;

public class ImException extends RuntimeException {

    private final int status;

    private final String error;

    public ImException(int status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public int status() {
        return status;
    }

    public String error() {
        return error;
    }

    public static ImException missing() {
        return new ImException(404, "RESOURCE_NOT_FOUND", "资源不存在或无权访问");
    }

    public static ImException invalid(String message) {
        return new ImException(400, "INVALID_ARGUMENT", message);
    }

    public static ImException conflict(String error, String message) {
        return new ImException(409, error, message);
    }

}

package org.smart.erp.common.result;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;

    private String msg;

    private T data;

    public Result(Integer code , String msg , T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(
                200,
                "请求成功",
                data
        );
    }

    public static <T> Result<T> fail(Integer code , String msg) {
        return new Result<T>(
                code,
                msg,
                null
        );
    }



}

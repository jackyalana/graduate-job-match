package com.lanrecruitment.common;

import com.lanrecruitment.common.enums.ErrorCode;

public class Response<T> {
    private Integer code;
    private String message;
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> Response<T> ok(T data) {
        Response<T> resp = new Response<>();
        resp.setCode(0);
        resp.setMessage("OK");
        resp.setData(data);
        return resp;
    }

    public static <T> Response<T> fail(Integer code, String message) {
        Response<T> resp = new Response<>();
        resp.setCode(code);
        resp.setMessage(message);
        resp.setData(null);
        return resp;
    }

    public static <T> Response<T> fail(ErrorCode errorCode) {
        if (errorCode == null) {
            return fail((Integer) null, null);
        }
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Response<T> fail(ErrorCode errorCode, String message) {
        if (errorCode == null) {
            return fail((Integer) null, message);
        }
        return fail(errorCode.getCode(), message);
    }
}

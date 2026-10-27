package com.lanrecruitment.common;

import com.lanrecruitment.common.enums.ErrorCode;
import org.slf4j.MDC;

public class Response<T> {
    private String code;
    private String message;
    private T data;
    private String requestId;
    private Object details;

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getRequestId() { return requestId; }
    public Object getDetails() { return details; }
    public Response<T> withDetails(Object value) { this.details=value; return this; }

    public static <T> Response<T> ok(T data) {
        Response<T> response = new Response<>();
        response.code = "OK"; response.message = "OK"; response.data = data;
        response.requestId = MDC.get("requestId");
        return response;
    }
    public static <T> Response<T> fail(ErrorCode error, String message) {
        Response<T> response = new Response<>();
        response.code = error == null ? "SYSTEM_ERROR" : error.name();
        response.message = message; response.requestId = MDC.get("requestId");
        return response;
    }
    public static <T> Response<T> fail(ErrorCode error) { return fail(error, error.getMessage()); }
}

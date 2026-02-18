package com.lanrecruitment.exception;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.common.enums.ErrorCode;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Response<Void> handleBizException(BizException e) {
        return Response.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Response.fail(ErrorCode.VALIDATION_FAILED, msg);
    }

    @ExceptionHandler(BindException.class)
    public Response<Void> handleBindException(BindException e) {
        String msg = e.getAllErrors().isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : e.getAllErrors().get(0).getDefaultMessage();
        return Response.fail(ErrorCode.VALIDATION_FAILED, msg);
    }

    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        return Response.fail(ErrorCode.SYSTEM_ERROR);
    }
}

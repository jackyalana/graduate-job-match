package com.lanrecruitment.exception;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.common.enums.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Response<Void>> business(BizException e) {
        ErrorCode code = e.getErrorCode() == null ? ErrorCode.SYSTEM_ERROR : e.getErrorCode();
        int status = code.getCode();
        if (code == ErrorCode.VALIDATION_FAILED || code == ErrorCode.FILE_TYPE_NOT_SUPPORTED || code == ErrorCode.FILE_EMPTY) status = 422;
        if (code == ErrorCode.FILE_TOO_LARGE) status = 413;
        if (code == ErrorCode.APPLY_DUPLICATE) status = 409;
        return ResponseEntity.status(status).body(Response.fail(code, e.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<Response<Void>> validation(Exception e) {
        return ResponseEntity.unprocessableEntity().body(Response.fail(ErrorCode.VALIDATION_FAILED));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response<Void>> tooLarge(Exception e) {
        return ResponseEntity.status(413).body(Response.fail(ErrorCode.FILE_TOO_LARGE));
    }
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Response<Void>> duplicate(Exception e) {
        return ResponseEntity.status(409).body(Response.fail(ErrorCode.APPLY_DUPLICATE));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> unexpected(Exception e) {
        return ResponseEntity.status(500).body(Response.fail(ErrorCode.SYSTEM_ERROR));
    }
}

package com.lanrecruitment.exception;

import com.lanrecruitment.common.enums.ErrorCode;

public class BizException extends RuntimeException {
    private final Integer code;
    private final ErrorCode errorCode;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode == null ? null : errorCode.getMessage());
        this.code = errorCode == null ? null : errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode == null ? null : errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, Object... messageArgs) {
        super(formatMessage(errorCode, messageArgs));
        this.code = errorCode == null ? null : errorCode.getCode();
        this.errorCode = errorCode;
    }

    public Integer getCode() {
        return code;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String formatMessage(ErrorCode errorCode, Object... messageArgs) {
        if (errorCode == null) {
            return null;
        }
        String template = errorCode.getMessage();
        if (messageArgs == null || messageArgs.length == 0) {
            return template;
        }
        try {
            return String.format(template, messageArgs);
        } catch (Exception ignored) {
            return template;
        }
    }
}

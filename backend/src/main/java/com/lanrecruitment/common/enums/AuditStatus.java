package com.lanrecruitment.common.enums;

public enum AuditStatus {
    PENDING(0),
    PASS(1),
    REJECT(2);

    private final int code;

    AuditStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package com.lanrecruitment.common.enums;

public enum CommonStatus {
    DISABLED(0),
    ENABLED(1);

    private final int code;

    CommonStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package com.lanrecruitment.common.enums;

public enum ApplyStatus {
    APPLIED(0),
    VIEWED(1),
    INTERVIEW(2),
    REJECT(3),
    OFFERED(4),
    ACCEPTED(5),
    DECLINED(6),
    WITHDRAWN(7);

    private final int code;

    ApplyStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (ApplyStatus s : values()) {
            if (s.code == code) {
                return true;
            }
        }
        return false;
    }
}

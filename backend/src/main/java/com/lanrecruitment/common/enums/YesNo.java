package com.lanrecruitment.common.enums;

public enum YesNo {
    NO(0),
    YES(1);

    private final int code;

    YesNo(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

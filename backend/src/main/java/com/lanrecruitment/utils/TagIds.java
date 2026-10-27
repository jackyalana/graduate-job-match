package com.lanrecruitment.utils;

import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.exception.BizException;
import java.util.ArrayList;
import java.util.List;

/** Null or blank means no filter; otherwise every comma-separated ID must be positive. */
public final class TagIds {
    private TagIds() {}
    public static List<Long> parse(String raw) {
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return ids;
        for (String value : raw.split(",", -1)) {
            String part = value.trim();
            if (!part.matches("[0-9]+")) throw new BizException(ErrorCode.VALIDATION_FAILED);
            try {
                long id = Long.parseLong(part);
                if (id <= 0) throw new BizException(ErrorCode.VALIDATION_FAILED);
                ids.add(id);
            } catch (NumberFormatException e) {
                throw new BizException(ErrorCode.VALIDATION_FAILED);
            }
        }
        return ids;
    }
}

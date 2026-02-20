package com.lanrecruitment.cache;

import java.math.BigDecimal;

public interface MatchScoreCache {
    BigDecimal get(Long resumeId, Long jobId);

    void put(Long resumeId, Long jobId, BigDecimal score);

    void invalidateByResume(Long resumeId);

    void invalidateByJob(Long jobId);
}

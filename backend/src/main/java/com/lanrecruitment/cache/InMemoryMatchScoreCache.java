package com.lanrecruitment.cache;

import com.lanrecruitment.common.constants.MatchScoreCacheConstants;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMatchScoreCache implements MatchScoreCache {

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Long>> resumeIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Long>> jobIndex = new ConcurrentHashMap<>();

    @Override
    public BigDecimal get(Long resumeId, Long jobId) {
        if (resumeId == null || jobId == null) {
            return null;
        }
        String key = pairKey(resumeId, jobId);
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() >= entry.expireAtMillis) {
            removePair(resumeId, jobId);
            return null;
        }
        return entry.score;
    }

    @Override
    public void put(Long resumeId, Long jobId, BigDecimal score) {
        if (resumeId == null || jobId == null || score == null) {
            return;
        }
        long expireAtMillis = System.currentTimeMillis() + MatchScoreCacheConstants.TTL_SECONDS * 1000L;
        store.put(pairKey(resumeId, jobId), new Entry(score, expireAtMillis));
        resumeIndex.computeIfAbsent(resumeId, k -> ConcurrentHashMap.newKeySet()).add(jobId);
        jobIndex.computeIfAbsent(jobId, k -> ConcurrentHashMap.newKeySet()).add(resumeId);
    }

    @Override
    public void invalidateByResume(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        Set<Long> jobIds = resumeIndex.remove(resumeId);
        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }
        for (Long jobId : jobIds) {
            removePair(resumeId, jobId);
        }
    }

    @Override
    public void invalidateByJob(Long jobId) {
        if (jobId == null) {
            return;
        }
        Set<Long> resumeIds = jobIndex.remove(jobId);
        if (resumeIds == null || resumeIds.isEmpty()) {
            return;
        }
        for (Long resumeId : resumeIds) {
            removePair(resumeId, jobId);
        }
    }

    private void removePair(Long resumeId, Long jobId) {
        if (resumeId == null || jobId == null) {
            return;
        }
        store.remove(pairKey(resumeId, jobId));
        Set<Long> jobIds = resumeIndex.get(resumeId);
        if (jobIds != null) {
            jobIds.remove(jobId);
            if (jobIds.isEmpty()) {
                resumeIndex.remove(resumeId, jobIds);
            }
        }
        Set<Long> resumeIds = jobIndex.get(jobId);
        if (resumeIds != null) {
            resumeIds.remove(resumeId);
            if (resumeIds.isEmpty()) {
                jobIndex.remove(jobId, resumeIds);
            }
        }
    }

    private static String pairKey(Long resumeId, Long jobId) {
        return MatchScoreCacheConstants.KEY_PREFIX + "score:" + resumeId + ":" + jobId;
    }

    private static final class Entry {
        private final BigDecimal score;
        private final long expireAtMillis;

        private Entry(BigDecimal score, long expireAtMillis) {
            this.score = score;
            this.expireAtMillis = expireAtMillis;
        }
    }
}

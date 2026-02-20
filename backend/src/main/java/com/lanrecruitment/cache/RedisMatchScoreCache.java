package com.lanrecruitment.cache;

import com.lanrecruitment.cache.redis.RedisClient;
import com.lanrecruitment.common.constants.MatchScoreCacheConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RedisMatchScoreCache implements MatchScoreCache {

    private final RedisClient redisClient;

    public RedisMatchScoreCache(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public BigDecimal get(Long resumeId, Long jobId) {
        if (resumeId == null || jobId == null) {
            return null;
        }
        String raw = redisClient.get(pairKey(resumeId, jobId));
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void put(Long resumeId, Long jobId, BigDecimal score) {
        if (resumeId == null || jobId == null || score == null) {
            return;
        }
        String resumeKey = resumeIndexKey(resumeId);
        String jobKey = jobIndexKey(jobId);

        redisClient.setEx(pairKey(resumeId, jobId), score.toPlainString(), MatchScoreCacheConstants.TTL_SECONDS);
        redisClient.sadd(resumeKey, String.valueOf(jobId));
        redisClient.expire(resumeKey, MatchScoreCacheConstants.TTL_SECONDS);
        redisClient.sadd(jobKey, String.valueOf(resumeId));
        redisClient.expire(jobKey, MatchScoreCacheConstants.TTL_SECONDS);
    }

    @Override
    public void invalidateByResume(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        String resumeKey = resumeIndexKey(resumeId);
        Set<String> jobIds = redisClient.smembers(resumeKey);
        if (!jobIds.isEmpty()) {
            List<String> keysToDelete = new ArrayList<>(jobIds.size());
            for (String jobIdStr : jobIds) {
                Long jobId = parseLong(jobIdStr);
                if (jobId == null) {
                    continue;
                }
                keysToDelete.add(pairKey(resumeId, jobId));
                redisClient.srem(jobIndexKey(jobId), String.valueOf(resumeId));
            }
            redisClient.del(keysToDelete);
        }
        redisClient.del(resumeKey);
    }

    @Override
    public void invalidateByJob(Long jobId) {
        if (jobId == null) {
            return;
        }
        String jobKey = jobIndexKey(jobId);
        Set<String> resumeIds = redisClient.smembers(jobKey);
        if (!resumeIds.isEmpty()) {
            List<String> keysToDelete = new ArrayList<>(resumeIds.size());
            for (String resumeIdStr : resumeIds) {
                Long resumeId = parseLong(resumeIdStr);
                if (resumeId == null) {
                    continue;
                }
                keysToDelete.add(pairKey(resumeId, jobId));
                redisClient.srem(resumeIndexKey(resumeId), String.valueOf(jobId));
            }
            redisClient.del(keysToDelete);
        }
        redisClient.del(jobKey);
    }

    private static Long parseLong(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String pairKey(Long resumeId, Long jobId) {
        return MatchScoreCacheConstants.KEY_PREFIX + "score:" + resumeId + ":" + jobId;
    }

    private static String resumeIndexKey(Long resumeId) {
        return MatchScoreCacheConstants.KEY_PREFIX + "resume:" + resumeId;
    }

    private static String jobIndexKey(Long jobId) {
        return MatchScoreCacheConstants.KEY_PREFIX + "job:" + jobId;
    }
}

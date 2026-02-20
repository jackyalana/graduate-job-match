package com.lanrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.cache.MatchScoreCache;
import com.lanrecruitment.domain.entity.JobTag;
import com.lanrecruitment.domain.entity.MatchScore;
import com.lanrecruitment.domain.entity.ResumeTag;
import com.lanrecruitment.mapper.JobTagMapper;
import com.lanrecruitment.mapper.MatchScoreMapper;
import com.lanrecruitment.mapper.ResumeTagMapper;
import com.lanrecruitment.service.MatchService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchServiceImpl implements MatchService {

    private final ResumeTagMapper resumeTagMapper;
    private final JobTagMapper jobTagMapper;
    private final MatchScoreMapper matchScoreMapper;
    private final MatchScoreCache matchScoreCache;

    public MatchServiceImpl(
            ResumeTagMapper resumeTagMapper,
            JobTagMapper jobTagMapper,
            MatchScoreMapper matchScoreMapper,
            MatchScoreCache matchScoreCache
    ) {
        this.resumeTagMapper = resumeTagMapper;
        this.jobTagMapper = jobTagMapper;
        this.matchScoreMapper = matchScoreMapper;
        this.matchScoreCache = matchScoreCache;
    }

    @Override
    public BigDecimal computeAndSave(Long resumeId, Long jobId) {
        BigDecimal cached = matchScoreCache.get(resumeId, jobId);
        if (cached != null) {
            return cached;
        }
        Map<Long, Integer> resumeVec = loadResumeVector(resumeId);
        Map<Long, Integer> jobVec = loadJobVector(jobId);
        BigDecimal score = cosine(resumeVec, jobVec);
        matchScoreCache.put(resumeId, jobId, score);

        MatchScore exists = matchScoreMapper.selectOne(new LambdaQueryWrapper<MatchScore>()
                .eq(MatchScore::getResumeId, resumeId)
                .eq(MatchScore::getJobId, jobId));
        if (exists == null) {
            MatchScore ms = new MatchScore();
            ms.setResumeId(resumeId);
            ms.setJobId(jobId);
            ms.setScore(score);
            ms.setCreatedAt(LocalDateTime.now());
            matchScoreMapper.insert(ms);
            return score;
        }

        exists.setScore(score);
        exists.setCreatedAt(LocalDateTime.now());
        matchScoreMapper.updateById(exists);
        return score;
    }

    private Map<Long, Integer> loadResumeVector(Long resumeId) {
        List<ResumeTag> list = resumeTagMapper.selectList(new LambdaQueryWrapper<ResumeTag>()
                .eq(ResumeTag::getResumeId, resumeId));
        Map<Long, Integer> map = new HashMap<>();
        for (ResumeTag rt : list) {
            if (rt.getTagId() == null) {
                continue;
            }
            Integer w = rt.getWeight() == null ? 0 : rt.getWeight();
            map.put(rt.getTagId(), w);
        }
        return map;
    }

    private Map<Long, Integer> loadJobVector(Long jobId) {
        List<JobTag> list = jobTagMapper.selectList(new LambdaQueryWrapper<JobTag>()
                .eq(JobTag::getJobId, jobId));
        Map<Long, Integer> map = new HashMap<>();
        for (JobTag jt : list) {
            if (jt.getTagId() == null) {
                continue;
            }
            Integer w = jt.getWeight() == null ? 0 : jt.getWeight();
            map.put(jt.getTagId(), w);
        }
        return map;
    }

    private BigDecimal cosine(Map<Long, Integer> a, Map<Long, Integer> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        Map<Long, Integer> small = a.size() <= b.size() ? a : b;
        Map<Long, Integer> large = a.size() <= b.size() ? b : a;

        double dot = 0.0;
        for (Map.Entry<Long, Integer> e : small.entrySet()) {
            Integer bw = large.get(e.getKey());
            if (bw == null) {
                continue;
            }
            dot += (double) e.getValue() * (double) bw;
        }

        double normA = 0.0;
        for (Integer w : a.values()) {
            normA += (double) w * (double) w;
        }

        double normB = 0.0;
        for (Integer w : b.values()) {
            normB += (double) w * (double) w;
        }

        if (normA == 0.0 || normB == 0.0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        double score = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        if (Double.isNaN(score) || Double.isInfinite(score) || score < 0) {
            score = 0.0;
        }
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }
}

package com.lanrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.cache.MatchScoreCache;
import com.lanrecruitment.domain.entity.MatchScore;
import com.lanrecruitment.mapper.JobTagMapper;
import com.lanrecruitment.mapper.MatchScoreMapper;
import com.lanrecruitment.mapper.ResumeTagMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

public class MatchServiceImplTest {

    @Test
    void shouldReturnL1CacheScore() {
        ResumeTagMapper resumeTagMapper = Mockito.mock(ResumeTagMapper.class);
        JobTagMapper jobTagMapper = Mockito.mock(JobTagMapper.class);
        MatchScoreMapper matchScoreMapper = Mockito.mock(MatchScoreMapper.class);
        MatchScoreCache matchScoreCache = Mockito.mock(MatchScoreCache.class);
        Mockito.when(matchScoreCache.get(1L, 2L)).thenReturn(new BigDecimal("0.3333"));

        MatchServiceImpl service = new MatchServiceImpl(resumeTagMapper, jobTagMapper, matchScoreMapper, matchScoreCache);
        BigDecimal score = service.computeAndSave(1L, 2L);

        assertThat(score).isEqualByComparingTo("0.3333");
        Mockito.verifyNoInteractions(matchScoreMapper);
        Mockito.verifyNoInteractions(resumeTagMapper);
        Mockito.verifyNoInteractions(jobTagMapper);
    }

    @Test
    void shouldReturnL2DbScoreAndBackfillCache() {
        ResumeTagMapper resumeTagMapper = Mockito.mock(ResumeTagMapper.class);
        JobTagMapper jobTagMapper = Mockito.mock(JobTagMapper.class);
        MatchScoreMapper matchScoreMapper = Mockito.mock(MatchScoreMapper.class);
        MatchScoreCache matchScoreCache = Mockito.mock(MatchScoreCache.class);
        Mockito.when(matchScoreCache.get(1L, 2L)).thenReturn(null);

        MatchScore ms = new MatchScore();
        ms.setResumeId(1L);
        ms.setJobId(2L);
        ms.setScore(new BigDecimal("0.4444"));
        Mockito.when(matchScoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ms);

        MatchServiceImpl service = new MatchServiceImpl(resumeTagMapper, jobTagMapper, matchScoreMapper, matchScoreCache);
        BigDecimal score = service.computeAndSave(1L, 2L);

        assertThat(score).isEqualByComparingTo("0.4444");
        Mockito.verify(matchScoreCache).put(1L, 2L, new BigDecimal("0.4444"));
        Mockito.verifyNoInteractions(resumeTagMapper);
        Mockito.verifyNoInteractions(jobTagMapper);
    }
}

package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.domain.entity.Job;
import com.lanrecruitment.domain.entity.JobTag;
import com.lanrecruitment.domain.entity.Resume;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.JobMapper;
import com.lanrecruitment.mapper.JobTagMapper;
import com.lanrecruitment.mapper.ResumeMapper;
import com.lanrecruitment.service.JobBrowseService;
import com.lanrecruitment.service.MatchService;
import com.lanrecruitment.domain.vo.JobCardVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class JobBrowseServiceImpl implements JobBrowseService {

    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final ResumeMapper resumeMapper;
    private final MatchService matchService;

    public JobBrowseServiceImpl(JobMapper jobMapper, JobTagMapper jobTagMapper, ResumeMapper resumeMapper, MatchService matchService) {
        this.jobMapper = jobMapper;
        this.jobTagMapper = jobTagMapper;
        this.resumeMapper = resumeMapper;
        this.matchService = matchService;
    }

    @Override
    public List<JobCardVO> listPublic(String keyword, String city, String jobType, String tagIds, Long cursorId, Integer pageSize) {
        int size = pageSize == null ? 10 : pageSize;
        if (size < 1) size = 1;
        if (size > 50) size = 50;
        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 1)
                .eq(Job::getAuditStatus, 1);
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(w -> w.like(Job::getJobName, keyword).or().like(Job::getDescription, keyword));
        }
        if (city != null && !city.trim().isEmpty()) {
            qw.eq(Job::getCity, city);
        }
        if (jobType != null && !jobType.trim().isEmpty()) {
            qw.eq(Job::getJobType, jobType);
        }
        if (cursorId != null) {
            qw.lt(Job::getId, cursorId);
        }
        List<Long> tagIdList = parseIds(tagIds);
        if (!tagIdList.isEmpty()) {
            List<Long> jobIds = jobTagMapper.selectList(new LambdaQueryWrapper<JobTag>().in(JobTag::getTagId, tagIdList))
                    .stream()
                    .map(JobTag::getJobId)
                    .distinct()
                    .collect(Collectors.toList());
            if (jobIds.isEmpty()) {
                return new ArrayList<>();
            }
            qw.in(Job::getId, jobIds);
        }
        List<Job> list = jobMapper.selectList(qw.orderByDesc(Job::getId).last("limit " + size));
        List<JobCardVO> res = new ArrayList<>();
        for (Job j : list) {
            res.add(toCard(j, null));
        }
        return res;
    }

    @Override
    public JobCardVO detail(Long jobId) {
        Job j = jobMapper.selectById(jobId);
        if (j == null || j.getStatus() == null || j.getStatus() != 1 || j.getAuditStatus() == null || j.getAuditStatus() != 1) {
            throw new BizException(404, "岗位不存在");
        }
        return toCard(j, null);
    }

    @Override
    public List<JobCardVO> recommend(Long resumeId, String keyword, String city, String jobType, String tagIds, Integer offset, Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        Resume resume = resolveResume(userId, resumeId);

        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, 1)
                .eq(Job::getAuditStatus, 1);
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(w -> w.like(Job::getJobName, keyword).or().like(Job::getDescription, keyword));
        }
        if (city != null && !city.trim().isEmpty()) {
            qw.eq(Job::getCity, city);
        }
        if (jobType != null && !jobType.trim().isEmpty()) {
            qw.eq(Job::getJobType, jobType);
        }
        List<Long> tagIdList = parseIds(tagIds);
        if (!tagIdList.isEmpty()) {
            List<Long> jobIds = jobTagMapper.selectList(new LambdaQueryWrapper<JobTag>().in(JobTag::getTagId, tagIdList))
                    .stream()
                    .map(JobTag::getJobId)
                    .distinct()
                    .collect(Collectors.toList());
            if (jobIds.isEmpty()) {
                return new ArrayList<>();
            }
            qw.in(Job::getId, jobIds);
        }

        if (resume == null) {
            int off = offset == null ? 0 : offset;
            if (off < 0) off = 0;
            int size = pageSize == null ? 10 : pageSize;
            if (size < 1) size = 1;
            if (size > 50) size = 50;
            qw.last("order by rand(" + userId + ") limit " + off + ", " + size);
            List<Job> jobs = jobMapper.selectList(qw);
            List<JobCardVO> res = new ArrayList<>();
            for (Job j : jobs) {
                res.add(toCard(j, null));
            }
            return res;
        }

        int candidateLimit = 200;
        List<Job> jobs = jobMapper.selectList(qw.orderByDesc(Job::getId).last("limit " + candidateLimit));

        List<JobCardVO> res = new ArrayList<>();
        for (Job j : jobs) {
            BigDecimal score = matchService.computeAndSave(resume.getId(), j.getId());
            res.add(toCard(j, score));
        }
        res.sort(Comparator
                .comparing(JobCardVO::getMatchScore, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(Comparator.comparing(JobCardVO::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed()));
        int off = offset == null ? 0 : offset;
        if (off < 0) off = 0;
        int size = pageSize == null ? 10 : pageSize;
        if (size < 1) size = 1;
        if (size > 50) size = 50;
        if (off >= res.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(res.size(), off + size);
        return new ArrayList<>(res.subList(off, end));
    }

    private Resume resolveResume(Long userId, Long resumeId) {
        Resume resume;
        if (resumeId != null) {
            resume = resumeMapper.selectById(resumeId);
            if (resume == null || resume.getUserId() == null || !resume.getUserId().equals(userId)) {
                throw new BizException(404, "简历不存在");
            }
            return resume;
        }

        resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .eq(Resume::getIsDefault, 1)
                .last("limit 1"));
        if (resume != null) {
            return resume;
        }

        resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdateTime)
                .last("limit 1"));
        return resume;
    }

    private JobCardVO toCard(Job j, BigDecimal score) {
        JobCardVO vo = new JobCardVO();
        vo.setId(j.getId());
        vo.setJobName(j.getJobName());
        vo.setCompanyName(j.getCompanyName());
        vo.setCity(j.getCity());
        vo.setSalaryRange(j.getSalaryRange());
        vo.setJobType(j.getJobType());
        vo.setDescription(j.getDescription());
        vo.setMatchScore(score);
        vo.setCreatedAt(j.getCreatedAt());
        return vo;
    }

    private List<Long> parseIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = raw.split(",");
        List<Long> res = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim();
            if (s.isEmpty()) continue;
            try {
                res.add(Long.parseLong(s));
            } catch (Exception ignored) {
            }
        }
        return res;
    }
}

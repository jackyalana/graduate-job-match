package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.YesNo;
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
    private final com.lanrecruitment.service.JobEligibilityService eligibility;
    private final com.lanrecruitment.service.RecommendationService recommendations;

    public JobBrowseServiceImpl(JobMapper jobMapper, JobTagMapper jobTagMapper, ResumeMapper resumeMapper, MatchService matchService, com.lanrecruitment.service.JobEligibilityService eligibility, com.lanrecruitment.service.RecommendationService recommendations) {
        this.jobMapper = jobMapper;
        this.jobTagMapper = jobTagMapper;
        this.resumeMapper = resumeMapper;
        this.matchService = matchService;
        this.eligibility = eligibility;
        this.recommendations = recommendations;
    }

    @Override
    public List<JobCardVO> listPublic(String keyword, String city, String jobType, String tagIds, Long cursorId, Integer pageSize, Long categoryId, Integer salaryMin, Integer salaryMax) {
        int size = pageSize == null ? 10 : pageSize;
        if (size < 1) size = 1;
        if (size > 50) size = 50;
        LambdaQueryWrapper<Job> qw = publicQuery(keyword,city,jobType,tagIds,categoryId,salaryMin,salaryMax);
        if (cursorId != null) qw.lt(Job::getId,cursorId);
        List<Job> list = jobMapper.selectList(qw.orderByDesc(Job::getId).last("limit " + size));
        List<JobCardVO> res = new ArrayList<>();
        for (Job j : list) {
            res.add(toCard(j, null));
        }
        return res;
    }

    private LambdaQueryWrapper<Job> publicQuery(String keyword,String city,String jobType,String tagIds,Long categoryId,Integer salaryMin,Integer salaryMax) {
        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<>();
        eligibility.visibleQuery(qw);
        if ((salaryMin!=null && salaryMin<0) || (salaryMax!=null && salaryMax<0) ||
            (salaryMin!=null && salaryMax!=null && salaryMin>salaryMax) || (categoryId!=null && categoryId<=0))
            throw new BizException(ErrorCode.VALIDATION_FAILED);
        if (categoryId!=null) qw.eq(Job::getCategoryId,categoryId);
        if (salaryMin!=null) qw.ge(Job::getSalaryMax,salaryMin);
        if (salaryMax!=null) qw.le(Job::getSalaryMin,salaryMax);
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(w -> w.like(Job::getJobName, keyword).or().like(Job::getDescription, keyword));
        }
        if (city != null && !city.trim().isEmpty()) {
            qw.and(w -> w.eq(Job::getCityCode,city).or().eq(Job::getCity,city));
        }
        if (jobType != null && !jobType.trim().isEmpty()) {
            qw.eq(Job::getJobType, jobType);
        }
        List<Long> tagIdList = com.lanrecruitment.utils.TagIds.parse(tagIds);
        if (!tagIdList.isEmpty()) {
            List<Long> jobIds = jobTagMapper.selectList(new LambdaQueryWrapper<JobTag>().in(JobTag::getTagId, tagIdList))
                    .stream()
                    .map(JobTag::getJobId)
                    .distinct()
                    .collect(Collectors.toList());
            if (jobIds.isEmpty()) {
                qw.eq(Job::getId,-1L);
                return qw;
            }
            qw.in(Job::getId, jobIds);
        }
        return qw;
    }

    @Override
    public com.lanrecruitment.domain.vo.JobPageVO searchPublic(String keyword,String city,String jobType,String tagIds,Long categoryId,Integer salaryMin,Integer salaryMax,int page,int pageSize) {
        if (page<1 || pageSize<1 || pageSize>50) throw new BizException(ErrorCode.VALIDATION_FAILED);
        LambdaQueryWrapper<Job> query=publicQuery(keyword,city,jobType,tagIds,categoryId,salaryMin,salaryMax);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Job> result=jobMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page,pageSize),query.orderByAsc(Job::getId));
        java.util.List<JobCardVO> items=new ArrayList<>();
        for (Job job:result.getRecords()) items.add(toCard(job,null));
        return new com.lanrecruitment.domain.vo.JobPageVO(items,result.getTotal(),page,pageSize);
    }

    @Override
    public JobCardVO detail(Long jobId) {
        Job j = jobMapper.selectById(jobId);
        if (!eligibility.publiclyVisible(j)) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        return toCard(j, null);
    }

    @Override
    public List<JobCardVO> recommend(Long resumeId, String keyword, String city, String jobType, String tagIds, Integer offset, Integer pageSize, Long categoryId, Integer salaryMin, Integer salaryMax) {
        com.lanrecruitment.domain.dto.RecommendationRequest r=new com.lanrecruitment.domain.dto.RecommendationRequest();
        r.setResumeId(resumeId);r.setKeyword(keyword);r.setCity(city);r.setJobType(jobType);r.setTagIds(tagIds);r.setCategoryId(categoryId);r.setSalaryMin(salaryMin);r.setSalaryMax(salaryMax);
        return recommendations.legacy(r,offset==null?0:Math.max(0,offset),pageSize==null?10:Math.max(1,Math.min(50,pageSize)));
    }

    private Resume resolveResume(Long userId, Long resumeId) {
        Resume resume;
        if (resumeId != null) {
            resume = resumeMapper.selectById(resumeId);
            if (resume == null || resume.getUserId() == null || !resume.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.RESUME_NOT_FOUND);
            }
            if (!Integer.valueOf(1).equals(resume.getStatus())) throw new BizException(ErrorCode.RESUME_NOT_AVAILABLE);
            return resume;
        }

        resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .eq(Resume::getIsDefault, YesNo.YES.getCode())
                .eq(Resume::getStatus, CommonStatus.ENABLED.getCode())
                .last("limit 1"));
        if (resume != null) {
            return resume;
        }

        return null;
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
        vo.setCategoryId(j.getCategoryId()); vo.setCityCode(j.getCityCode());
        vo.setSalaryMin(j.getSalaryMin()); vo.setSalaryMax(j.getSalaryMax()); vo.setSalaryNegotiable(j.getSalaryNegotiable());
        vo.setEducationMinRank(j.getEducationMinRank()); vo.setGraduateYearMin(j.getGraduateYearMin());
        vo.setGraduateYearMax(j.getGraduateYearMax()); vo.setAcceptFreshGraduate(j.getAcceptFreshGraduate()); vo.setDeadline(j.getDeadline());
        vo.setMatchScore(score);
        vo.setCreatedAt(j.getCreatedAt());
        return vo;
    }

}

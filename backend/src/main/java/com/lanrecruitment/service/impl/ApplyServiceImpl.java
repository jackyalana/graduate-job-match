package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.ApplyStatus;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.dto.ApplyJobDTO;
import com.lanrecruitment.domain.dto.ApplyResumeUpdateDTO;
import com.lanrecruitment.domain.entity.Job;
import com.lanrecruitment.domain.entity.JobApply;
import com.lanrecruitment.domain.entity.Resume;
import com.lanrecruitment.domain.vo.JobCardVO;
import com.lanrecruitment.domain.vo.ResumeVO;
import com.lanrecruitment.domain.vo.UserApplyVO;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.JobApplyMapper;
import com.lanrecruitment.mapper.JobMapper;
import com.lanrecruitment.mapper.ResumeMapper;
import com.lanrecruitment.service.ApplyService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Service;

@Service
public class ApplyServiceImpl implements ApplyService {

    private final JobApplyMapper jobApplyMapper;
    private final ResumeMapper resumeMapper;
    private final JobMapper jobMapper;

    public ApplyServiceImpl(JobApplyMapper jobApplyMapper, ResumeMapper resumeMapper, JobMapper jobMapper) {
        this.jobApplyMapper = jobApplyMapper;
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
    }

    @Override
    public void apply(ApplyJobDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        Resume resume = resumeMapper.selectById(dto.getResumeId());
        if (resume == null || resume.getUserId() == null || !resume.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.RESUME_NOT_FOUND);
        }
        if (resume.getStatus() == null || resume.getStatus() != CommonStatus.ENABLED.getCode()) {
            throw new BizException(ErrorCode.RESUME_NOT_AVAILABLE);
        }

        Job job = jobMapper.selectById(dto.getJobId());
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        if (job.getStatus() == null || job.getStatus() != CommonStatus.ENABLED.getCode()
                || job.getAuditStatus() == null || job.getAuditStatus() != AuditStatus.PASS.getCode()) {
            throw new BizException(ErrorCode.JOB_NOT_APPLICABLE);
        }

        Long cnt = jobApplyMapper.selectCount(new LambdaQueryWrapper<JobApply>()
                .eq(JobApply::getResumeId, dto.getResumeId())
                .eq(JobApply::getJobId, dto.getJobId()));
        if (cnt != null && cnt > 0) {
            throw new BizException(ErrorCode.APPLY_DUPLICATE);
        }

        JobApply apply = new JobApply();
        apply.setJobId(dto.getJobId());
        apply.setResumeId(dto.getResumeId());
        apply.setUserId(userId);
        apply.setStatus(ApplyStatus.APPLIED.getCode());
        apply.setCreatedAt(LocalDateTime.now());
        jobApplyMapper.insert(apply);
    }

    @Override
    public List<JobApply> listMy() {
        Long userId = StpUtil.getLoginIdAsLong();
        return jobApplyMapper.selectList(new LambdaQueryWrapper<JobApply>()
                .eq(JobApply::getUserId, userId)
                .orderByDesc(JobApply::getId));
    }

    @Override
    public List<UserApplyVO> listMyDetail() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<JobApply> applies = jobApplyMapper.selectList(new LambdaQueryWrapper<JobApply>()
                .eq(JobApply::getUserId, userId)
                .orderByDesc(JobApply::getId));

        Set<Long> jobIds = new HashSet<>();
        Set<Long> resumeIds = new HashSet<>();
        for (JobApply a : applies) {
            if (a.getJobId() != null) jobIds.add(a.getJobId());
            if (a.getResumeId() != null) resumeIds.add(a.getResumeId());
        }

        List<Job> jobs = jobIds.isEmpty() ? new ArrayList<>() : jobMapper.selectBatchIds(jobIds);
        Map<Long, Job> jobMap = new HashMap<>();
        for (Job j : jobs) {
            jobMap.put(j.getId(), j);
        }

        List<Resume> resumes = resumeIds.isEmpty() ? new ArrayList<>() : resumeMapper.selectBatchIds(resumeIds);
        Map<Long, Resume> resumeMap = new HashMap<>();
        for (Resume r : resumes) {
            resumeMap.put(r.getId(), r);
        }

        List<UserApplyVO> res = new ArrayList<>();
        for (JobApply a : applies) {
            UserApplyVO vo = new UserApplyVO();
            vo.setApplyId(a.getId());
            vo.setApplyStatus(a.getStatus());
            vo.setApplyTime(a.getCreatedAt());
            Job j = jobMap.get(a.getJobId());
            if (j != null) {
                vo.setJob(toJobCardVO(j));
            }
            Resume r = resumeMap.get(a.getResumeId());
            if (r != null) {
                vo.setResume(toResumeVO(r));
            }
            res.add(vo);
        }
        return res;
    }

    @Override
    public void updateApplyResume(ApplyResumeUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        JobApply apply = jobApplyMapper.selectById(dto.getApplyId());
        if (apply == null || apply.getUserId() == null || !apply.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        }
        Integer status = apply.getStatus();
        if (status != null && status >= ApplyStatus.INTERVIEW.getCode()) {
            throw new BizException(ErrorCode.APPLY_CANNOT_CHANGE_RESUME);
        }

        Resume resume = resumeMapper.selectById(dto.getResumeId());
        if (resume == null || resume.getUserId() == null || !resume.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.RESUME_NOT_FOUND);
        }
        if (resume.getStatus() == null || resume.getStatus() != CommonStatus.ENABLED.getCode()) {
            throw new BizException(ErrorCode.RESUME_NOT_AVAILABLE);
        }

        Long cnt = jobApplyMapper.selectCount(new LambdaQueryWrapper<JobApply>()
                .eq(JobApply::getUserId, userId)
                .eq(JobApply::getJobId, apply.getJobId())
                .eq(JobApply::getResumeId, dto.getResumeId())
                .ne(JobApply::getId, apply.getId()));
        if (cnt != null && cnt > 0) {
            throw new BizException(ErrorCode.APPLY_DUPLICATE);
        }

        apply.setResumeId(dto.getResumeId());
        jobApplyMapper.updateById(apply);
    }

    private JobCardVO toJobCardVO(Job j) {
        JobCardVO vo = new JobCardVO();
        vo.setId(j.getId());
        vo.setJobName(j.getJobName());
        vo.setCompanyName(j.getCompanyName());
        vo.setCity(j.getCity());
        vo.setSalaryRange(j.getSalaryRange());
        vo.setJobType(j.getJobType());
        vo.setDescription(j.getDescription());
        vo.setCreatedAt(j.getCreatedAt());
        return vo;
    }

    private ResumeVO toResumeVO(Resume r) {
        ResumeVO vo = new ResumeVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setResumeName(r.getResumeName());
        vo.setRealName(r.getRealName());
        vo.setGender(r.getGender());
        vo.setCity(r.getCity());
        vo.setEducation(r.getEducation());
        vo.setWorkYears(r.getWorkYears());
        vo.setExpectJobType(r.getExpectJobType());
        vo.setExpectSalary(r.getExpectSalary());
        vo.setWorkDesc(r.getWorkDesc());
        vo.setFileName(r.getFileName());
        vo.setFileUrl(r.getFileUrl());
        vo.setFileType(r.getFileType());
        vo.setFileSize(r.getFileSize());
        vo.setIsDefault(r.getIsDefault());
        vo.setStatus(r.getStatus());
        vo.setCreateTime(r.getCreateTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }
}

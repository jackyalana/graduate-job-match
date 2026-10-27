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
    private final com.lanrecruitment.service.JobEligibilityService eligibility;
    private final ApplicationWorkflowService workflow;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public ApplyServiceImpl(JobApplyMapper jobApplyMapper, ResumeMapper resumeMapper, JobMapper jobMapper, com.lanrecruitment.service.JobEligibilityService eligibility, ApplicationWorkflowService workflow, com.fasterxml.jackson.databind.ObjectMapper json) {
        this.jobApplyMapper = jobApplyMapper;
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
        this.eligibility = eligibility;
        this.workflow=workflow;this.json=json;
    }

    @Override
    public void apply(ApplyJobDTO dto) {
        workflow.submit(dto.getJobId(),dto.getResumeId());
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
            vo.setVersion(a.getVersion());
            vo.setApplyTime(a.getCreatedAt());
            Job j = jobMap.get(a.getJobId());
            Resume r = resumeMap.get(a.getResumeId());
            try {
                if(a.getJobSnapshot()!=null)j=json.treeToValue(json.readTree(a.getJobSnapshot()).get("job"),Job.class);
                if(a.getResumeSnapshot()!=null)r=json.treeToValue(json.readTree(a.getResumeSnapshot()).get("resume"),Resume.class);
            } catch(java.io.IOException e) {throw new BizException(ErrorCode.SYSTEM_ERROR);}
            if (j != null) {
                vo.setJob(toJobCardVO(j));
            }
            if (r != null) {
                vo.setResume(toResumeVO(r));
            }
            vo.setHasAttachment(a.getAttachmentSnapshot()!=null);
            res.add(vo);
        }
        return res;
    }

    @Override
    public void updateApplyResume(ApplyResumeUpdateDTO dto) {
        throw new BizException(ErrorCode.CONFLICT, "投递后简历快照不可替换");
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

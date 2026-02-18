package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.ApplyStatus;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.dto.ApplyStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.JobSaveDTO;
import com.lanrecruitment.domain.entity.Job;
import com.lanrecruitment.domain.entity.JobApply;
import com.lanrecruitment.domain.entity.JobTag;
import com.lanrecruitment.domain.entity.Resume;
import com.lanrecruitment.domain.entity.ResumeTag;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.domain.entity.Tag;
import com.lanrecruitment.domain.vo.HrJobApplicationVO;
import com.lanrecruitment.domain.vo.HrJobVO;
import com.lanrecruitment.domain.vo.JobTagVO;
import com.lanrecruitment.domain.vo.ResumeTagVO;
import com.lanrecruitment.domain.vo.ResumeVO;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.JobApplyMapper;
import com.lanrecruitment.mapper.JobMapper;
import com.lanrecruitment.mapper.JobTagMapper;
import com.lanrecruitment.mapper.ResumeMapper;
import com.lanrecruitment.mapper.ResumeTagMapper;
import com.lanrecruitment.mapper.SysUserMapper;
import com.lanrecruitment.mapper.TagMapper;
import com.lanrecruitment.service.HrJobService;
import com.lanrecruitment.service.MatchService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HrJobServiceImpl implements HrJobService {

    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final JobApplyMapper jobApplyMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeTagMapper resumeTagMapper;
    private final SysUserMapper sysUserMapper;
    private final TagMapper tagMapper;
    private final MatchService matchService;

    public HrJobServiceImpl(
            JobMapper jobMapper,
            JobTagMapper jobTagMapper,
            JobApplyMapper jobApplyMapper,
            ResumeMapper resumeMapper,
            ResumeTagMapper resumeTagMapper,
            SysUserMapper sysUserMapper,
            TagMapper tagMapper,
            MatchService matchService
    ) {
        this.jobMapper = jobMapper;
        this.jobTagMapper = jobTagMapper;
        this.jobApplyMapper = jobApplyMapper;
        this.resumeMapper = resumeMapper;
        this.resumeTagMapper = resumeTagMapper;
        this.sysUserMapper = sysUserMapper;
        this.tagMapper = tagMapper;
        this.matchService = matchService;
    }

    @Override
    public List<HrJobVO> listMy() {
        Long hrId = StpUtil.getLoginIdAsLong();
        List<Job> list = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getHrId, hrId)
                .orderByDesc(Job::getId));

        Set<Long> jobIds = new HashSet<>();
        for (Job j : list) {
            if (j.getId() != null) {
                jobIds.add(j.getId());
            }
        }

        Map<Long, Integer> applyCountMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            List<JobApply> allApplies = jobApplyMapper.selectList(new LambdaQueryWrapper<JobApply>().in(JobApply::getJobId, jobIds));
            for (JobApply a : allApplies) {
                applyCountMap.merge(a.getJobId(), 1, Integer::sum);
            }
        }

        List<HrJobVO> res = new ArrayList<>();
        for (Job j : list) {
            HrJobVO vo = new HrJobVO();
            vo.setId(j.getId());
            vo.setJobName(j.getJobName());
            vo.setCompanyName(j.getCompanyName());
            vo.setCity(j.getCity());
            vo.setSalaryRange(j.getSalaryRange());
            vo.setJobType(j.getJobType());
            vo.setStatus(j.getStatus());
            vo.setAuditStatus(j.getAuditStatus());
            vo.setCreatedAt(j.getCreatedAt());
            vo.setApplyCount(applyCountMap.getOrDefault(j.getId(), 0));
            res.add(vo);
        }
        return res;
    }

    @Override
    public void save(JobSaveDTO dto) {
        Long hrId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();
        SysUser hr = sysUserMapper.selectById(hrId);
        String companyName = hr == null ? null : hr.getCompanyName();
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new BizException(ErrorCode.HR_COMPANY_NAME_REQUIRED);
        }

        if (dto.getId() == null) {
            Job j = new Job();
            j.setHrId(hrId);
            j.setJobName(dto.getJobName());
            j.setCompanyName(companyName.trim());
            j.setCity(dto.getCity());
            j.setSalaryRange(dto.getSalaryRange());
            j.setJobType(dto.getJobType());
            j.setDescription(dto.getDescription());
            j.setStatus(dto.getStatus());
            j.setAuditStatus(AuditStatus.PENDING.getCode());
            j.setCreatedAt(now);
            j.setUpdatedAt(now);
            jobMapper.insert(j);
            return;
        }

        Job exists = getMyJob(dto.getId());
        exists.setJobName(dto.getJobName());
        exists.setCompanyName(companyName.trim());
        exists.setCity(dto.getCity());
        exists.setSalaryRange(dto.getSalaryRange());
        exists.setJobType(dto.getJobType());
        exists.setDescription(dto.getDescription());
        exists.setStatus(dto.getStatus());
        exists.setAuditStatus(AuditStatus.PENDING.getCode());
        exists.setUpdatedAt(now);
        jobMapper.updateById(exists);
    }

    @Override
    public void delete(IdDTO dto) {
        Job job = getMyJob(dto.getId());
        job.setStatus(CommonStatus.DISABLED.getCode());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
    }

    @Override
    public List<JobTagVO> listJobTags(Long jobId) {
        Job job = getMyJob(jobId);
        List<JobTag> list = jobTagMapper.selectList(new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId, job.getId()));
        Set<Long> tagIds = new HashSet<>();
        for (JobTag jt : list) {
            if (jt.getTagId() != null) {
                tagIds.add(jt.getTagId());
            }
        }
        List<Tag> tags = tagIds.isEmpty() ? new ArrayList<>() : tagMapper.selectBatchIds(tagIds);
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag t : tags) {
            tagMap.put(t.getId(), t);
        }

        List<JobTagVO> res = new ArrayList<>();
        for (JobTag jt : list) {
            JobTagVO vo = new JobTagVO();
            vo.setTagId(jt.getTagId());
            vo.setProficiency(jt.getProficiency());
            vo.setWeight(jt.getWeight());
            Tag t = tagMap.get(jt.getTagId());
            if (t != null) {
                vo.setTagName(t.getTagName());
                vo.setTagType(t.getTagType());
            }
            res.add(vo);
        }
        return res;
    }

    @Override
    public List<HrJobApplicationVO> listApplications(Long jobId) {
        Job job = getMyJob(jobId);
        List<JobApply> applies = jobApplyMapper.selectList(new LambdaQueryWrapper<JobApply>()
                .eq(JobApply::getJobId, job.getId())
                .orderByDesc(JobApply::getId));
        Set<Long> resumeIds = new HashSet<>();
        for (JobApply a : applies) {
            if (a.getResumeId() != null) {
                resumeIds.add(a.getResumeId());
            }
        }
        List<Resume> resumes = resumeIds.isEmpty() ? new ArrayList<>() : resumeMapper.selectBatchIds(resumeIds);
        Map<Long, Resume> resumeMap = new HashMap<>();
        for (Resume r : resumes) {
            resumeMap.put(r.getId(), r);
        }

        Map<Long, List<ResumeTagVO>> resumeTagMap = new HashMap<>();
        if (!resumeIds.isEmpty()) {
            List<ResumeTag> rts = resumeTagMapper.selectList(new LambdaQueryWrapper<ResumeTag>().in(ResumeTag::getResumeId, resumeIds));
            Set<Long> tagIds = new HashSet<>();
            for (ResumeTag rt : rts) {
                if (rt.getTagId() != null) {
                    tagIds.add(rt.getTagId());
                }
            }
            List<Tag> tags = tagIds.isEmpty() ? new ArrayList<>() : tagMapper.selectBatchIds(tagIds);
            Map<Long, Tag> tagMap = new HashMap<>();
            for (Tag t : tags) {
                tagMap.put(t.getId(), t);
            }

            for (ResumeTag rt : rts) {
                ResumeTagVO tv = new ResumeTagVO();
                tv.setTagId(rt.getTagId());
                tv.setProficiency(rt.getProficiency());
                tv.setWeight(rt.getWeight());
                Tag t = tagMap.get(rt.getTagId());
                if (t != null) {
                    tv.setTagName(t.getTagName());
                    tv.setTagType(t.getTagType());
                }
                resumeTagMap.computeIfAbsent(rt.getResumeId(), k -> new ArrayList<>()).add(tv);
            }
        }

        List<HrJobApplicationVO> res = new ArrayList<>();
        for (JobApply a : applies) {
            HrJobApplicationVO vo = new HrJobApplicationVO();
            vo.setApplyId(a.getId());
            vo.setApplyStatus(a.getStatus());
            vo.setApplyTime(a.getCreatedAt());

            Resume r = resumeMap.get(a.getResumeId());
            if (r != null) {
                vo.setResume(toResumeVO(r));
                vo.setMatchScore(matchService.computeAndSave(r.getId(), job.getId()));
            }
            vo.setResumeTags(resumeTagMap.getOrDefault(a.getResumeId(), new ArrayList<>()));
            res.add(vo);
        }
        res.sort(Comparator.comparing(HrJobApplicationVO::getMatchScore, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return res;
    }

    @Override
    public void updateApplyStatus(ApplyStatusUpdateDTO dto) {
        JobApply apply = jobApplyMapper.selectById(dto.getApplyId());
        if (apply == null) {
            throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        }
        getMyJob(apply.getJobId());
        Integer s = dto.getStatus();
        if (!ApplyStatus.isValid(s)) {
            throw new BizException(ErrorCode.APPLY_STATUS_INVALID);
        }
        if (ApplyStatus.VIEWED.getCode() == s) {
            throw new BizException(ErrorCode.APPLY_STATUS_VIEWED_UNSUPPORTED);
        }
        apply.setStatus(s);
        jobApplyMapper.updateById(apply);
    }

    private Job getMyJob(Long jobId) {
        Long hrId = StpUtil.getLoginIdAsLong();
        Job job = jobMapper.selectById(jobId);
        if (job == null || job.getHrId() == null || !job.getHrId().equals(hrId)) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        return job;
    }

    private static ResumeVO toResumeVO(Resume r) {
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

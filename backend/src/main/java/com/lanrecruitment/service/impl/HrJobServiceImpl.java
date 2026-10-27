package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.cache.MatchScoreCache;
import com.lanrecruitment.common.enums.ApplyStatus;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.TagType;
import com.lanrecruitment.domain.dto.ApplyStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.JobSaveDTO;
import com.lanrecruitment.domain.dto.JobTagItemDTO;
import com.lanrecruitment.domain.dto.JobTagSaveDTO;
import com.lanrecruitment.domain.entity.Job;
import com.lanrecruitment.domain.entity.JobApply;
import com.lanrecruitment.domain.entity.JobTag;
import com.lanrecruitment.domain.entity.MatchScore;
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
import com.lanrecruitment.mapper.MatchScoreMapper;
import com.lanrecruitment.mapper.ResumeMapper;
import com.lanrecruitment.mapper.ResumeTagMapper;
import com.lanrecruitment.mapper.SysUserMapper;
import com.lanrecruitment.mapper.TagMapper;
import com.lanrecruitment.service.FileStorageService;
import com.lanrecruitment.service.HrJobService;
import com.lanrecruitment.service.MatchService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrJobServiceImpl implements HrJobService {

    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final JobApplyMapper jobApplyMapper;
    private final MatchScoreMapper matchScoreMapper;
    private final MatchScoreCache matchScoreCache;
    private final ResumeMapper resumeMapper;
    private final ResumeTagMapper resumeTagMapper;
    private final SysUserMapper sysUserMapper;
    private final TagMapper tagMapper;
    private final MatchService matchService;
    private final FileStorageService fileStorageService;
    private final com.lanrecruitment.service.JobCategoryService categories;
    private final com.lanrecruitment.service.RecommendationService recommendations;

    public HrJobServiceImpl(
            JobMapper jobMapper,
            JobTagMapper jobTagMapper,
            JobApplyMapper jobApplyMapper,
            MatchScoreMapper matchScoreMapper,
            MatchScoreCache matchScoreCache,
            ResumeMapper resumeMapper,
            ResumeTagMapper resumeTagMapper,
            SysUserMapper sysUserMapper,
            TagMapper tagMapper,
            MatchService matchService,
            FileStorageService fileStorageService,
            com.lanrecruitment.service.JobCategoryService categories,
            com.lanrecruitment.service.RecommendationService recommendations
    ) {
        this.jobMapper = jobMapper;
        this.jobTagMapper = jobTagMapper;
        this.jobApplyMapper = jobApplyMapper;
        this.matchScoreMapper = matchScoreMapper;
        this.matchScoreCache = matchScoreCache;
        this.resumeMapper = resumeMapper;
        this.resumeTagMapper = resumeTagMapper;
        this.sysUserMapper = sysUserMapper;
        this.tagMapper = tagMapper;
        this.matchService = matchService;
        this.fileStorageService = fileStorageService;
        this.categories = categories;
        this.recommendations = recommendations;
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
            vo.setJobType(j.getJobType()); vo.setDescription(j.getDescription());
            vo.setCategoryId(j.getCategoryId()); vo.setCityCode(j.getCityCode()); vo.setSalaryMin(j.getSalaryMin());
            vo.setSalaryMax(j.getSalaryMax()); vo.setSalaryNegotiable(j.getSalaryNegotiable()); vo.setEducationMinRank(j.getEducationMinRank());
            vo.setGraduateYearMin(j.getGraduateYearMin()); vo.setGraduateYearMax(j.getGraduateYearMax());
            vo.setAcceptFreshGraduate(j.getAcceptFreshGraduate()); vo.setDeadline(j.getDeadline()); vo.setVersion(j.getVersion());
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
        categories.requireExisting(dto.getCategoryId());
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
            copyConditions(dto, j);
            j.setStatus(dto.getStatus());
            j.setAuditStatus(AuditStatus.PENDING.getCode());
            j.setCreatedAt(now);
            j.setUpdatedAt(now);
            jobMapper.insert(j);
            return;
        }

        Job exists = getMyJob(dto.getId());
        if (dto.getVersion()==null || !dto.getVersion().equals(exists.getVersion())) throw new BizException(ErrorCode.CONFLICT);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Job> update = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Job>()
          .eq(Job::getId,exists.getId()).eq(Job::getHrId,hrId).eq(Job::getVersion,dto.getVersion())
          .set(Job::getJobName,dto.getJobName()).set(Job::getCompanyName,companyName.trim())
          .set(Job::getCity,dto.getCity()).set(Job::getSalaryRange,dto.getSalaryRange()).set(Job::getJobType,dto.getJobType())
          .set(Job::getDescription,dto.getDescription()).set(Job::getStatus,dto.getStatus())
          .set(Job::getCategoryId,dto.getCategoryId()).set(Job::getCityCode,dto.getCityCode())
          .set(Job::getSalaryMin,dto.getSalaryMin()).set(Job::getSalaryMax,dto.getSalaryMax()).set(Job::getSalaryNegotiable,dto.getSalaryNegotiable())
          .set(Job::getEducationMinRank,dto.getEducationMinRank()).set(Job::getGraduateYearMin,dto.getGraduateYearMin())
          .set(Job::getGraduateYearMax,dto.getGraduateYearMax()).set(Job::getAcceptFreshGraduate,dto.getAcceptFreshGraduate())
          .set(Job::getDeadline,dto.getDeadline()).set(Job::getAuditStatus,AuditStatus.PENDING.getCode())
          .set(Job::getVersion,dto.getVersion()+1).set(Job::getUpdatedAt,now);
        if (jobMapper.update(null,update)!=1) throw new BizException(ErrorCode.CONFLICT);
    }

    private void copyConditions(JobSaveDTO dto, Job j) {
        j.setCategoryId(dto.getCategoryId()); j.setCityCode(dto.getCityCode()); j.setSalaryMin(dto.getSalaryMin());
        j.setSalaryMax(dto.getSalaryMax()); j.setSalaryNegotiable(dto.getSalaryNegotiable()); j.setEducationMinRank(dto.getEducationMinRank());
        j.setGraduateYearMin(dto.getGraduateYearMin()); j.setGraduateYearMax(dto.getGraduateYearMax());
        j.setAcceptFreshGraduate(dto.getAcceptFreshGraduate()); j.setDeadline(dto.getDeadline());
    }

    @Override
    public void delete(IdDTO dto) {
        Job job = getMyJob(dto.getId());
        job.setStatus(CommonStatus.DISABLED.getCode());
        job.setVersion(job.getVersion()+1);
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
    }

    @Transactional
    @Override
    public void remove(IdDTO dto) {
        // The old remove endpoint becomes a close operation and never deletes applications.
        delete(dto);
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

    @Transactional
    @Override
    public void saveJobTags(JobTagSaveDTO dto) {
        Job job = getMyJob(dto.getJobId());
        job = jobMapper.selectOne(new LambdaQueryWrapper<Job>().eq(Job::getId,job.getId()).last("FOR UPDATE"));
        job.setAuditStatus(AuditStatus.PENDING.getCode()); job.setVersion(job.getVersion()+1); job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        Long jobId = job.getId();
        if (jobId == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }

        jobTagMapper.delete(new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId, jobId));
        matchScoreMapper.delete(new LambdaQueryWrapper<MatchScore>().eq(MatchScore::getJobId, jobId));
        matchScoreCache.invalidateByJob(jobId);

        List<JobTagItemDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<Long> tagIds = new HashSet<>();
        for (JobTagItemDTO item : items) {
            if (item.getTagId() != null) {
                tagIds.add(item.getTagId());
            }
        }
        List<Tag> tags = tagIds.isEmpty() ? new ArrayList<>() : tagMapper.selectBatchIds(tagIds);
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag t : tags) {
            tagMap.put(t.getId(), t);
        }

        for (JobTagItemDTO item : items) {
            Tag t = tagMap.get(item.getTagId());
            if (t == null) {
                continue;
            }
            int base = t.getBaseWeight() == null ? 1 : t.getBaseWeight();
            int proficiency = item.getProficiency() == null ? 1 : item.getProficiency();
            int weight = base;
            if (TagType.isSkill(t.getTagType())) {
                weight = base * proficiency;
            }

            JobTag jt = new JobTag();
            jt.setJobId(jobId);
            jt.setTagId(item.getTagId());
            jt.setProficiency(TagType.isSkill(t.getTagType()) ? proficiency : null);
            jt.setWeight(weight);
            jobTagMapper.insert(jt);
        }
    }

    @Override
    public List<HrJobApplicationVO> listApplications(Long jobId) {
        Job job = getMyJob(jobId);
        List<JobApply> applies = jobApplyMapper.selectList(new LambdaQueryWrapper<JobApply>()
                .eq(JobApply::getJobId, job.getId())
                .orderByDesc(JobApply::getId));
        Set<Long> resumeIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (JobApply a : applies) {
            if (a.getResumeId() != null) {
                resumeIds.add(a.getResumeId());
            }
            if (a.getUserId() != null) {
                userIds.add(a.getUserId());
            }
        }
        List<Resume> resumes = resumeIds.isEmpty() ? new ArrayList<>() : resumeMapper.selectBatchIds(resumeIds);
        Map<Long, Resume> resumeMap = new HashMap<>();
        for (Resume r : resumes) {
            resumeMap.put(r.getId(), r);
        }

        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser u : users) {
                userMap.put(u.getId(), u);
            }
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
            SysUser user = userMap.get(a.getUserId());
            vo.setApplicantPhone(user == null ? null : user.getPhone());

            Resume r = resumeMap.get(a.getResumeId());
            if (r != null) {
                vo.setResume(toResumeVO(r));
                if (Integer.valueOf(1).equals(r.getStatus())) vo.setMatchScore(recommendations.applicantMatch(a.getUserId(),r.getId(),job.getId()));
            }
            vo.setResumeTags(resumeTagMap.getOrDefault(a.getResumeId(), new ArrayList<>()));
            res.add(vo);
        }
        res.sort(Comparator.comparing(HrJobApplicationVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));
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

    @Override
    public ResponseEntity<Resource> downloadResumeAttachment(Long applyId) {
        JobApply apply = jobApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        }
        getMyJob(apply.getJobId());
        Resume r = resumeMapper.selectById(apply.getResumeId());
        if (r == null) {
            throw new BizException(ErrorCode.RESUME_NOT_FOUND);
        }
        if (r.getFileUrl() == null || r.getFileUrl().trim().isEmpty()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        Path path = fileStorageService.resolveResumeAttachmentPath(r.getFileUrl());
        if (path == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        return buildDownloadResponse(path, r.getFileName());
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

    private ResponseEntity<Resource> buildDownloadResponse(Path path, String downloadName) {
        try {
            if (!Files.exists(path) || !Files.isReadable(path)) {
                throw new BizException(ErrorCode.FILE_NOT_FOUND);
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BizException(ErrorCode.FILE_NOT_FOUND);
            }
            String filename = (downloadName == null || downloadName.trim().isEmpty())
                    ? path.getFileName().toString()
                    : downloadName;
            String contentType = Files.probeContentType(path);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(Files.size(path))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8)
                            .build()
                            .toString())
                    .body(resource);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_READ_FAILED);
        }
    }
}

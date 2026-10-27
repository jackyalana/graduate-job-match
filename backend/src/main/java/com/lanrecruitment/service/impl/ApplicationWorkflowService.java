package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.dto.ApplyStatusUpdateDTO;
import com.lanrecruitment.domain.entity.*;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.*;
import com.lanrecruitment.service.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ApplicationWorkflowService {
    private final JobApplyMapper applies; private final JobMapper jobs; private final ResumeMapper resumes;
    private final ApplicationEventMapper events; private final BehaviorEventMapper behaviors;
    private final ResumeTagMapper resumeTags; private final JobTagMapper jobTags;
    private final TagMapper tags;
    private final StudentProfileService profiles; private final RecommendationService recommendations;
    private final JobEligibilityService eligibility; private final FileStorageService files; private final ObjectMapper json;
    public ApplicationWorkflowService(JobApplyMapper applies,JobMapper jobs,ResumeMapper resumes,ApplicationEventMapper events,
            BehaviorEventMapper behaviors,ResumeTagMapper resumeTags,JobTagMapper jobTags,TagMapper tags,StudentProfileService profiles,
            RecommendationService recommendations,JobEligibilityService eligibility,FileStorageService files,ObjectMapper json) {
        this.applies=applies;this.jobs=jobs;this.resumes=resumes;this.events=events;this.behaviors=behaviors;
        this.resumeTags=resumeTags;this.jobTags=jobTags;this.tags=tags;this.profiles=profiles;this.recommendations=recommendations;
        this.eligibility=eligibility;this.files=files;this.json=json;
    }
    @Transactional
    public void submit(Long jobId,Long resumeId) {
        Long user=StpUtil.getLoginIdAsLong();
        Job job=jobs.selectOne(new LambdaQueryWrapper<Job>().eq(Job::getId,jobId).last("FOR UPDATE"));
        if(!eligibility.publiclyVisible(job))throw new BizException(ErrorCode.JOB_NOT_APPLICABLE);
        Resume resume=resumes.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getId,resumeId).last("FOR UPDATE"));
        if(resume==null || !user.equals(resume.getUserId()))throw new BizException(ErrorCode.RESUME_NOT_FOUND);
        if(!Integer.valueOf(1).equals(resume.getStatus()))throw new BizException(ErrorCode.RESUME_NOT_AVAILABLE);
        if(applies.selectCount(new LambdaQueryWrapper<JobApply>().eq(JobApply::getUserId,user).eq(JobApply::getJobId,jobId))>0)throw new BizException(ErrorCode.APPLY_DUPLICATE);
        try {
            Map<String,Object> resumeData=new LinkedHashMap<>();resumeData.put("resume",resume);
            List<ResumeTag> capturedTags=resumeTags.selectList(new LambdaQueryWrapper<ResumeTag>().eq(ResumeTag::getResumeId,resumeId));
            resumeData.put("tags",capturedTags);
            List<Long> tagIds=new ArrayList<>();for(ResumeTag t:capturedTags)tagIds.add(t.getTagId());
            resumeData.put("tagDictionary",tagIds.isEmpty()?Collections.emptyList():tags.selectBatchIds(tagIds));
            Map<String,Object> jobData=new LinkedHashMap<>();jobData.put("job",job);
            List<JobTag> capturedJobTags=jobTags.selectList(new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId,jobId));
            jobData.put("tags",capturedJobTags);List<Long> jobTagIds=new ArrayList<>();for(JobTag t:capturedJobTags)jobTagIds.add(t.getTagId());
            jobData.put("tagDictionary",jobTagIds.isEmpty()?Collections.emptyList():tags.selectBatchIds(jobTagIds));
            Object match=recommendations.detail(jobId,resumeId);
            String attachment=null;
            if(resume.getFileUrl()!=null && !resume.getFileUrl().trim().isEmpty()) {
                Path original=files.resolveResumeAttachmentPath(resume.getFileUrl());
                if(original==null || !Files.isRegularFile(original) || !Files.isReadable(original))throw new BizException(ErrorCode.FILE_NOT_FOUND);
                String filename=UUID.randomUUID().toString().replace("-","")+".pdf";
                Path target=original.getParent().resolve(filename);Files.copy(original,target);
                attachment="/uploads/resume/"+filename;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(target);}catch(Exception ignored){} }
                });
            }
            LocalDateTime now=LocalDateTime.now().withNano(0);JobApply apply=new JobApply();apply.setUserId(user);apply.setJobId(jobId);apply.setResumeId(resumeId);
            apply.setStatus(0);apply.setVersion(1L);apply.setCreatedAt(now);apply.setProfileSnapshot(json.writeValueAsString(profiles.get()));
            apply.setResumeSnapshot(json.writeValueAsString(resumeData));apply.setJobSnapshot(json.writeValueAsString(jobData));
            apply.setMatchSnapshot(json.writeValueAsString(match));apply.setAttachmentSnapshot(attachment);applies.insert(apply);
            recordEvent(apply.getId(),null,0,user); recordBehavior(user,jobId,"APPLY", "apply:"+apply.getId());
        } catch(org.springframework.dao.DuplicateKeyException e){throw new BizException(ErrorCode.APPLY_DUPLICATE);}
          catch(BizException e){throw e;} catch(Exception e){throw new BizException(ErrorCode.SYSTEM_ERROR);}
    }
    @Transactional
    public void transition(ApplyStatusUpdateDTO dto,boolean hr) {
        Long actor=StpUtil.getLoginIdAsLong();JobApply apply=applies.selectById(dto.getApplyId());
        if(apply==null)throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        if(hr) {Job job=jobs.selectById(apply.getJobId());if(job==null || !actor.equals(job.getHrId()))throw new BizException(ErrorCode.JOB_NOT_FOUND);}
        else if(!actor.equals(apply.getUserId()))throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        if(dto.getVersion()==null || !dto.getVersion().equals(apply.getVersion()))throw new BizException(ErrorCode.CONFLICT);
        int from=apply.getStatus(),to=dto.getStatus();if(from==to)return;
        boolean early=from==0 || from==1 || from==2;
        boolean legal=hr ? ((from==0 && (to==1 || to==2)) || (from==1 && to==2) || (from==2 && to==4) || (early && to==3))
                         : ((early && to==7) || (from==4 && (to==5 || to==6)));
        if(!legal)throw new BizException(ErrorCode.APPLY_STATUS_INVALID);
        int changed=applies.update(null,new LambdaUpdateWrapper<JobApply>().eq(JobApply::getId,apply.getId()).eq(JobApply::getVersion,dto.getVersion()).eq(JobApply::getStatus,from).set(JobApply::getStatus,to).set(JobApply::getVersion,dto.getVersion()+1));
        if(changed!=1)throw new BizException(ErrorCode.CONFLICT);
        recordEvent(apply.getId(),from,to,actor);
    }
    private void recordEvent(Long id,Integer from,int to,Long actor) {ApplicationEvent e=new ApplicationEvent();e.setApplicationId(id);e.setFromStatus(from);e.setToStatus(to);e.setActorId(actor);e.setCreatedAt(LocalDateTime.now().withNano(0));events.insert(e);}
    public void recordBehavior(Long user,Long job,String type,String key) {BehaviorEvent e=new BehaviorEvent();e.setUserId(user);e.setJobId(job);e.setEventType(type);e.setEventTime(LocalDateTime.now().withNano(0));e.setDedupeKey(key);behaviors.insert(e);}
    public List<ApplicationEvent> timeline(Long id,boolean hr) {
        JobApply a=applies.selectById(id);Long actor=StpUtil.getLoginIdAsLong();if(a==null)throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        if(hr){Job j=jobs.selectById(a.getJobId());if(j==null || !actor.equals(j.getHrId()))throw new BizException(ErrorCode.JOB_NOT_FOUND);}
        else if(!actor.equals(a.getUserId()))throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        return events.selectList(new LambdaQueryWrapper<ApplicationEvent>().eq(ApplicationEvent::getApplicationId,id).orderByAsc(ApplicationEvent::getId));
    }
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadOwnAttachment(Long id) {
        JobApply a=applies.selectById(id);if(a==null || !Long.valueOf(StpUtil.getLoginIdAsLong()).equals(a.getUserId()))throw new BizException(ErrorCode.APPLY_RECORD_NOT_FOUND);
        if(a.getAttachmentSnapshot()==null)throw new BizException(ErrorCode.FILE_NOT_FOUND);
        try {Path path=files.resolveResumeAttachmentPath(a.getAttachmentSnapshot());if(!Files.isRegularFile(path))throw new BizException(ErrorCode.FILE_NOT_FOUND);
            return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header("Content-Disposition","attachment; filename=resume-snapshot.pdf").body(new org.springframework.core.io.UrlResource(path.toUri()));
        } catch(BizException e){throw e;} catch(Exception e){throw new BizException(ErrorCode.FILE_READ_FAILED);}
    }
}

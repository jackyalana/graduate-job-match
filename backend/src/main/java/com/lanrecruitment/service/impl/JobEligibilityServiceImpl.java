package com.lanrecruitment.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.domain.entity.*;
import com.lanrecruitment.mapper.*;
import com.lanrecruitment.service.JobEligibilityService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
@Service
public class JobEligibilityServiceImpl implements JobEligibilityService {
    private final SysUserMapper users;
    private final StudentProfileMapper profiles;
    public JobEligibilityServiceImpl(SysUserMapper users, StudentProfileMapper profiles) { this.users=users; this.profiles=profiles; }
    @Override public void visibleQuery(LambdaQueryWrapper<Job> query) {
        query.eq(Job::getStatus,1).eq(Job::getAuditStatus,1)
            .and(w -> w.isNull(Job::getDeadline).or().gt(Job::getDeadline,LocalDateTime.now()))
            .inSql(Job::getHrId,"SELECT id FROM sys_user WHERE role='HR' AND status=1 AND audit_status=1");
    }
    @Override public boolean publiclyVisible(Job job) {
        if (job==null || !Integer.valueOf(1).equals(job.getStatus()) || !Integer.valueOf(1).equals(job.getAuditStatus()) ||
                (job.getDeadline()!=null && !job.getDeadline().isAfter(LocalDateTime.now()))) return false;
        SysUser hr=users.selectById(job.getHrId());
        return hr!=null && "HR".equals(hr.getRole()) && Integer.valueOf(1).equals(hr.getStatus()) && Integer.valueOf(1).equals(hr.getAuditStatus());
    }
    @Override public void personalQuery(LambdaQueryWrapper<Job> query, Long userId) {
        personalQueryIncludingApplied(query,userId);
        query.notInSql(Job::getId,"SELECT job_id FROM job_apply WHERE user_id="+userId);
    }
    @Override public void personalQueryIncludingApplied(LambdaQueryWrapper<Job> query, Long userId) {
        visibleQuery(query);
        query.eq(Job::getAcceptFreshGraduate,true);
        StudentProfile p=profiles.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId,userId));
        Integer edu=p==null?null:p.getEducationRank(), year=p==null?null:p.getGraduationYear();
        if (edu==null) query.isNull(Job::getEducationMinRank);
        else query.and(w -> w.isNull(Job::getEducationMinRank).or().le(Job::getEducationMinRank,edu));
        if (year==null) query.isNull(Job::getGraduateYearMin).isNull(Job::getGraduateYearMax);
        else query.and(w -> w.isNull(Job::getGraduateYearMin).or().le(Job::getGraduateYearMin,year))
                  .and(w -> w.isNull(Job::getGraduateYearMax).or().ge(Job::getGraduateYearMax,year));
    }
}

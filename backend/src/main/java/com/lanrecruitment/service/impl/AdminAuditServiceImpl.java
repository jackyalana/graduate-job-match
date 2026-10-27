package com.lanrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.UserRole;
import com.lanrecruitment.domain.dto.AuditDecisionDTO;
import com.lanrecruitment.domain.entity.Job;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.JobMapper;
import com.lanrecruitment.mapper.SysUserMapper;
import com.lanrecruitment.service.AdminAuditService;
import com.lanrecruitment.domain.vo.AdminHrAuditVO;
import com.lanrecruitment.domain.vo.AdminJobAuditVO;
import com.lanrecruitment.domain.vo.AdminJobDetailVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    private final SysUserMapper sysUserMapper;
    private final JobMapper jobMapper;
    private final com.lanrecruitment.mapper.JobTagMapper jobTagMapper;
    private final com.lanrecruitment.mapper.TagMapper tagMapper;

    public AdminAuditServiceImpl(SysUserMapper sysUserMapper, JobMapper jobMapper, com.lanrecruitment.mapper.JobTagMapper jobTagMapper, com.lanrecruitment.mapper.TagMapper tagMapper) {
        this.sysUserMapper = sysUserMapper;
        this.jobMapper = jobMapper;
        this.jobTagMapper = jobTagMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public List<AdminHrAuditVO> listPendingHr() {
        List<SysUser> list = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, UserRole.HR.name())
                .eq(SysUser::getAuditStatus, AuditStatus.PENDING.getCode())
                .orderByDesc(SysUser::getId));
        List<AdminHrAuditVO> res = new ArrayList<>();
        for (SysUser u : list) {
            AdminHrAuditVO vo = new AdminHrAuditVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setEmail(u.getEmail());
            vo.setCompanyName(u.getCompanyName());
            vo.setStatus(u.getStatus());
            vo.setAuditStatus(u.getAuditStatus());
            vo.setCreatedAt(u.getCreatedAt());
            res.add(vo);
        }
        return res;
    }

    @Override
    public void auditHr(AuditDecisionDTO dto) {
        SysUser u = sysUserMapper.selectById(dto.getId());
        if (u == null || UserRole.from(u.getRole()) != UserRole.HR) {
            throw new BizException(ErrorCode.HR_ACCOUNT_NOT_FOUND);
        }
        u.setAuditStatus(dto.getPass() ? AuditStatus.PASS.getCode() : AuditStatus.REJECT.getCode());
        u.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(u);
    }

    @Override
    public List<AdminJobAuditVO> listPendingJob() {
        List<Job> list = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getAuditStatus, AuditStatus.PENDING.getCode())
                .orderByDesc(Job::getId));
        List<AdminJobAuditVO> res = new ArrayList<>();
        for (Job j : list) {
            AdminJobAuditVO vo = new AdminJobAuditVO();
            vo.setId(j.getId());
            vo.setHrId(j.getHrId());
            vo.setVersion(j.getVersion());
            vo.setJobName(j.getJobName());
            vo.setCity(j.getCity());
            vo.setSalaryRange(j.getSalaryRange());
            vo.setJobType(j.getJobType());
            vo.setStatus(j.getStatus());
            vo.setAuditStatus(j.getAuditStatus());
            vo.setCreatedAt(j.getCreatedAt());
            res.add(vo);
        }
        return res;
    }

    @Override
    public void auditJob(AuditDecisionDTO dto) {
        Job job = jobMapper.selectById(dto.getId());
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        if (dto.getVersion()==null || !dto.getVersion().equals(job.getVersion())) throw new BizException(ErrorCode.CONFLICT);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Job> update=new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Job>()
            .eq(Job::getId,job.getId()).eq(Job::getAuditStatus,AuditStatus.PENDING.getCode()).eq(Job::getVersion,dto.getVersion())
            .set(Job::getAuditStatus,dto.getPass() ? AuditStatus.PASS.getCode() : AuditStatus.REJECT.getCode())
            .set(Job::getVersion,dto.getVersion()+1).set(Job::getUpdatedAt,LocalDateTime.now());
        if (jobMapper.update(null,update)!=1) throw new BizException(ErrorCode.CONFLICT);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public AdminJobDetailVO getJobDetail(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        SysUser hr = job.getHrId() == null ? null : sysUserMapper.selectById(job.getHrId());
        List<com.lanrecruitment.domain.vo.JobTagVO> tags = new ArrayList<>();
        for (com.lanrecruitment.domain.entity.JobTag jt : jobTagMapper.selectList(new LambdaQueryWrapper<com.lanrecruitment.domain.entity.JobTag>().eq(com.lanrecruitment.domain.entity.JobTag::getJobId, id))) {
            com.lanrecruitment.domain.entity.Tag tag = tagMapper.selectById(jt.getTagId());
            tags.add(com.lanrecruitment.domain.vo.JobTagVO.builder().tagId(jt.getTagId()).tagName(tag == null ? null : tag.getTagName()).tagType(tag == null ? null : tag.getTagType()).proficiency(jt.getProficiency()).weight(jt.getWeight()).build());
        }
        return AdminJobDetailVO.builder()
                .id(job.getId())
                .hrId(job.getHrId())
                .hrUsername(hr == null ? null : hr.getUsername())
                .jobName(job.getJobName())
                .companyName(job.getCompanyName())
                .city(job.getCity())
                .salaryRange(job.getSalaryRange())
                .jobType(job.getJobType())
                .description(job.getDescription())
                .version(job.getVersion()).categoryId(job.getCategoryId()).cityCode(job.getCityCode())
                .salaryMin(job.getSalaryMin()).salaryMax(job.getSalaryMax()).salaryNegotiable(job.getSalaryNegotiable())
                .educationMinRank(job.getEducationMinRank()).graduateYearMin(job.getGraduateYearMin()).graduateYearMax(job.getGraduateYearMax())
                .acceptFreshGraduate(job.getAcceptFreshGraduate()).deadline(job.getDeadline()).tags(tags)
                .status(job.getStatus())
                .auditStatus(job.getAuditStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}

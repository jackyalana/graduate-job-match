package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.UserRole;
import com.lanrecruitment.domain.dto.AdminUserStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.entity.Job;
import com.lanrecruitment.domain.entity.JobApply;
import com.lanrecruitment.domain.entity.JobTag;
import com.lanrecruitment.domain.entity.LoginLog;
import com.lanrecruitment.domain.entity.MatchScore;
import com.lanrecruitment.domain.entity.Resume;
import com.lanrecruitment.domain.entity.ResumeTag;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.domain.vo.AdminUserVO;
import com.lanrecruitment.domain.vo.PageVO;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.JobApplyMapper;
import com.lanrecruitment.mapper.JobMapper;
import com.lanrecruitment.mapper.JobTagMapper;
import com.lanrecruitment.mapper.LoginLogMapper;
import com.lanrecruitment.mapper.MatchScoreMapper;
import com.lanrecruitment.mapper.ResumeMapper;
import com.lanrecruitment.mapper.ResumeTagMapper;
import com.lanrecruitment.mapper.SysUserMapper;
import com.lanrecruitment.service.AdminUserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeTagMapper resumeTagMapper;
    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final JobApplyMapper jobApplyMapper;
    private final MatchScoreMapper matchScoreMapper;
    private final LoginLogMapper loginLogMapper;
    private final AuditLogService auditLog;

    public AdminUserServiceImpl(
            SysUserMapper sysUserMapper,
            ResumeMapper resumeMapper,
            ResumeTagMapper resumeTagMapper,
            JobMapper jobMapper,
            JobTagMapper jobTagMapper,
            JobApplyMapper jobApplyMapper,
            MatchScoreMapper matchScoreMapper,
            LoginLogMapper loginLogMapper, AuditLogService auditLog
    ) {
        this.sysUserMapper = sysUserMapper;
        this.resumeMapper = resumeMapper;
        this.resumeTagMapper = resumeTagMapper;
        this.jobMapper = jobMapper;
        this.jobTagMapper = jobTagMapper;
        this.jobApplyMapper = jobApplyMapper;
        this.matchScoreMapper = matchScoreMapper;
        this.loginLogMapper = loginLogMapper;
        this.auditLog=auditLog;
    }

    @Override
    public PageVO<AdminUserVO> listUserPage(String role, Integer status, Integer auditStatus, String keyword, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        if (role != null && !role.trim().isEmpty()) {
            String r = role.trim().toUpperCase();
            if (!UserRole.USER.name().equals(r) && !UserRole.HR.name().equals(r)) {
                throw new BizException(ErrorCode.AUTH_ROLE_INVALID);
            }
            role = r;
        } else {
            role = null;
        }

        String kw = keyword == null ? null : keyword.trim();
        if (kw != null && kw.isEmpty()) {
            kw = null;
        }

        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        qw.ne(SysUser::getRole, UserRole.ADMIN.name());
        if (role != null) {
            qw.eq(SysUser::getRole, role);
        }
        if (status != null) {
            qw.eq(SysUser::getStatus, status);
        }
        if (auditStatus != null) {
            qw.eq(SysUser::getAuditStatus, auditStatus);
        }
        if (kw != null) {
            String like = kw;
            qw.and(w -> w.like(SysUser::getUsername, like)
                    .or().like(SysUser::getEmail, like)
                    .or().like(SysUser::getRealName, like)
                    .or().like(SysUser::getPhone, like)
                    .or().like(SysUser::getCompanyName, like));
        }
        qw.orderByDesc(SysUser::getId);

        Page<SysUser> p = new Page<>(page, pageSize);
        Page<SysUser> result = sysUserMapper.selectPage(p, qw);

        List<AdminUserVO> list = new ArrayList<>();
        for (SysUser u : result.getRecords()) {
            list.add(toAdminUserVO(u));
        }
        return new PageVO<>(list, result.getTotal(), page, pageSize);
    }

    @Transactional
    @Override
    public void updateStatus(AdminUserStatusUpdateDTO dto) {
        Long currentAdminId = StpUtil.getLoginIdAsLong();
        if (dto.getId() != null && dto.getId().equals(currentAdminId)) {
            throw new BizException(ErrorCode.ADMIN_CANNOT_OPERATE_SELF);
        }

        SysUser u = sysUserMapper.selectById(dto.getId());
        if (u == null) {
            throw new BizException(ErrorCode.ADMIN_USER_NOT_FOUND);
        }
        if (UserRole.ADMIN.name().equals(u.getRole())) {
            throw new BizException(ErrorCode.ADMIN_CANNOT_OPERATE_ADMIN);
        }

        u.setStatus(dto.getStatus());
        u.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(u);
        auditLog.record("USER",u.getId(),Integer.valueOf(0).equals(dto.getStatus())?"DISABLE":"ENABLE",null);
        if (Integer.valueOf(0).equals(dto.getStatus())) StpUtil.logout(u.getId());
    }

    @Transactional
    @Override
    public void deleteUser(IdDTO dto) {
        // M1 guard: the legacy endpoint must not physically delete application history.
        throw new BizException(ErrorCode.CONFLICT, "账号删除暂不可用，请停用账号");
    }

    private void deleteUserData(Long userId) {
        List<Resume> resumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId));
        Set<Long> resumeIds = new HashSet<>();
        for (Resume r : resumes) {
            if (r.getId() != null) {
                resumeIds.add(r.getId());
            }
        }

        if (!resumeIds.isEmpty()) {
            resumeTagMapper.delete(new LambdaQueryWrapper<ResumeTag>().in(ResumeTag::getResumeId, resumeIds));
            matchScoreMapper.delete(new LambdaQueryWrapper<MatchScore>().in(MatchScore::getResumeId, resumeIds));
            jobApplyMapper.delete(new LambdaQueryWrapper<JobApply>().in(JobApply::getResumeId, resumeIds));
            resumeMapper.deleteBatchIds(resumeIds);
        }

        jobApplyMapper.delete(new LambdaQueryWrapper<JobApply>().eq(JobApply::getUserId, userId));
    }

    private void deleteHrData(Long hrId) {
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>().eq(Job::getHrId, hrId));
        Set<Long> jobIds = new HashSet<>();
        for (Job j : jobs) {
            if (j.getId() != null) {
                jobIds.add(j.getId());
            }
        }

        if (!jobIds.isEmpty()) {
            jobTagMapper.delete(new LambdaQueryWrapper<JobTag>().in(JobTag::getJobId, jobIds));
            jobApplyMapper.delete(new LambdaQueryWrapper<JobApply>().in(JobApply::getJobId, jobIds));
            matchScoreMapper.delete(new LambdaQueryWrapper<MatchScore>().in(MatchScore::getJobId, jobIds));
            jobMapper.deleteBatchIds(jobIds);
        }
    }

    private static AdminUserVO toAdminUserVO(SysUser u) {
        if (u == null) return null;
        return AdminUserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .companyName(u.getCompanyName())
                .avatar(u.getAvatar())
                .status(u.getStatus())
                .auditStatus(u.getAuditStatus())
                .realName(u.getRealName())
                .phone(u.getPhone())
                .school(u.getSchool())
                .graduateDate(u.getGraduateDate())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}

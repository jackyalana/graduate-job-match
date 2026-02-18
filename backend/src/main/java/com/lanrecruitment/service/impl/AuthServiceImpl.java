package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.EmailPurpose;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.UserRole;
import com.lanrecruitment.domain.dto.LoginByEmailDTO;
import com.lanrecruitment.domain.dto.LoginByPasswordDTO;
import com.lanrecruitment.domain.dto.RegisterDTO;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.SysUserMapper;
import com.lanrecruitment.service.AuthService;
import com.lanrecruitment.service.EmailCodeService;
import com.lanrecruitment.utils.PasswordUtil;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final EmailCodeService emailCodeService;

    public AuthServiceImpl(SysUserMapper sysUserMapper, EmailCodeService emailCodeService) {
        this.sysUserMapper = sysUserMapper;
        this.emailCodeService = emailCodeService;
    }

    @Override
    public String loginByPassword(LoginByPasswordDTO dto) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != CommonStatus.ENABLED.getCode()) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (UserRole.isHr(user.getRole()) && (user.getAuditStatus() == null || user.getAuditStatus() != AuditStatus.PASS.getCode())) {
            throw new BizException(ErrorCode.AUTH_HR_PENDING_AUDIT);
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public String loginByEmail(LoginByEmailDTO dto) {
        emailCodeService.verifyCode(dto.getEmail(), EmailPurpose.LOGIN.name(), dto.getCode());
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail()));
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }
        if (user.getStatus() == null || user.getStatus() != CommonStatus.ENABLED.getCode()) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (UserRole.isHr(user.getRole()) && (user.getAuditStatus() == null || user.getAuditStatus() != AuditStatus.PASS.getCode())) {
            throw new BizException(ErrorCode.AUTH_HR_PENDING_AUDIT);
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void register(RegisterDTO dto) {
        emailCodeService.verifyCode(dto.getEmail(), EmailPurpose.REGISTER.name(), dto.getCode());
        UserRole role = dto.getRole() == null ? UserRole.USER : UserRole.from(dto.getRole());
        if (role == null || role == UserRole.ADMIN) {
            throw new BizException(ErrorCode.AUTH_ROLE_INVALID);
        }
        Long usernameCnt = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (usernameCnt != null && usernameCnt > 0) {
            throw new BizException(ErrorCode.AUTH_USERNAME_EXISTS);
        }
        Long emailCnt = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail()));
        if (emailCnt != null && emailCnt > 0) {
            throw new BizException(ErrorCode.AUTH_EMAIL_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser u = new SysUser();
        u.setUsername(dto.getUsername());
        u.setPassword(PasswordUtil.hash(dto.getPassword()));
        u.setEmail(dto.getEmail());
        u.setRole(role.name());
        u.setStatus(CommonStatus.ENABLED.getCode());
        u.setAuditStatus(role == UserRole.HR ? AuditStatus.PENDING.getCode() : AuditStatus.PASS.getCode());
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        sysUserMapper.insert(u);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}

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
    private final org.springframework.core.env.Environment environment;
    private final LoginRateLimiter loginRateLimiter;

    public AuthServiceImpl(SysUserMapper sysUserMapper, EmailCodeService emailCodeService,
            org.springframework.core.env.Environment environment, LoginRateLimiter loginRateLimiter) {
        this.sysUserMapper = sysUserMapper;
        this.emailCodeService = emailCodeService;
        this.environment = environment;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    public String loginByPassword(LoginByPasswordDTO dto) {
        loginRateLimiter.check(dto.getUsername());
        try { return doPasswordLogin(dto); }
        catch (BizException e) { loginRateLimiter.recordFailure(dto.getUsername()); throw e; }
    }

    private String doPasswordLogin(LoginByPasswordDTO dto) {
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
        loginRateLimiter.clear(dto.getUsername());
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public String loginByEmail(LoginByEmailDTO dto) {
        String limitKey="email:"+dto.getEmail();
        loginRateLimiter.check(limitKey);
        try { return doEmailLogin(dto,limitKey); }
        catch (BizException e) { loginRateLimiter.recordFailure(limitKey); throw e; }
    }

    private String doEmailLogin(LoginByEmailDTO dto, String limitKey) {
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
        loginRateLimiter.clear(limitKey);
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void register(RegisterDTO dto) {
        boolean demo = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("demo"))
                && environment.getProperty("lan.demo.local-registration",Boolean.class,false);
        if (demo) {
            org.springframework.web.context.request.ServletRequestAttributes attrs = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            String remote=attrs.getRequest().getRemoteAddr();
            if (!"127.0.0.1".equals(remote) && !"::1".equals(remote) && !"0:0:0:0:0:0:0:1".equals(remote))
                throw new BizException(ErrorCode.AUTH_ROLE_INVALID, "演示注册仅允许本机访问");
        } else {
            if (dto.getCode()==null || dto.getCode().trim().isEmpty()) throw new BizException(ErrorCode.EMAIL_CODE_INVALID);
            emailCodeService.verifyCode(dto.getEmail(), EmailPurpose.REGISTER.name(), dto.getCode());
        }
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
        u.setEmailVerified(!demo);
        u.setRole(role.name());
        if (role == UserRole.HR) {
            u.setCompanyName(dto.getCompanyName() == null ? null : dto.getCompanyName().trim());
        }
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

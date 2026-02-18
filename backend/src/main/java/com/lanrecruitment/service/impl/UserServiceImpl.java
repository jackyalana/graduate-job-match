package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.UserRole;
import com.lanrecruitment.domain.dto.ChangePasswordDTO;
import com.lanrecruitment.domain.dto.UpdateProfileDTO;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.SysUserMapper;
import com.lanrecruitment.service.UserService;
import com.lanrecruitment.utils.PasswordUtil;
import com.lanrecruitment.domain.vo.CurrentUserVO;
import com.lanrecruitment.domain.vo.UserProfileVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    public UserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public CurrentUserVO getMe() {
        SysUser u = currentUser();
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRole(u.getRole());
        return vo;
    }

    @Override
    public UserProfileVO getProfile() {
        SysUser u = currentUser();
        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setEmail(u.getEmail());
        vo.setRole(u.getRole());
        vo.setCompanyName(u.getCompanyName());
        vo.setAvatar(u.getAvatar());
        vo.setStatus(u.getStatus());
        vo.setAuditStatus(u.getAuditStatus());
        vo.setRealName(u.getRealName());
        vo.setPhone(u.getPhone());
        vo.setSchool(u.getSchool());
        vo.setGraduateDate(u.getGraduateDate());
        return vo;
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        SysUser u = currentUser();
        UserRole role = UserRole.from(u.getRole());
        if (role == UserRole.HR) {
            if (dto.getRealName() != null) u.setRealName(trimToNull(dto.getRealName()));
            if (dto.getPhone() != null) u.setPhone(trimToNull(dto.getPhone()));
        } else if (role == UserRole.USER) {
            if (dto.getRealName() != null) u.setRealName(trimToNull(dto.getRealName()));
            if (dto.getPhone() != null) u.setPhone(trimToNull(dto.getPhone()));
            if (dto.getSchool() != null) u.setSchool(trimToNull(dto.getSchool()));
            u.setGraduateDate(dto.getGraduateDate());
        } else {
            if (dto.getRealName() != null) u.setRealName(trimToNull(dto.getRealName()));
            if (dto.getPhone() != null) u.setPhone(trimToNull(dto.getPhone()));
            if (dto.getSchool() != null) u.setSchool(trimToNull(dto.getSchool()));
            u.setGraduateDate(dto.getGraduateDate());
        }
        u.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(u);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        SysUser u = currentUser();
        if (!PasswordUtil.matches(dto.getOldPassword(), u.getPassword())) {
            throw new BizException(ErrorCode.USER_OLD_PASSWORD_WRONG);
        }
        u.setPassword(PasswordUtil.hash(dto.getNewPassword()));
        u.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(u);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        SysUser u = currentUser();
        u.setAvatar(avatarUrl);
        u.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(u);
    }

    private SysUser currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser u = sysUserMapper.selectById(userId);
        if (u == null) {
            throw new BizException(ErrorCode.AUTH_NOT_LOGGED_IN);
        }
        return u;
    }

    private static String trimToNull(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isEmpty() ? null : s;
    }
}

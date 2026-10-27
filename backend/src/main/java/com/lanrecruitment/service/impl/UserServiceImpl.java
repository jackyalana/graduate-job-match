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
    private final com.lanrecruitment.mapper.StudentProfileMapper profiles;

    public UserServiceImpl(SysUserMapper sysUserMapper, com.lanrecruitment.mapper.StudentProfileMapper profiles) {
        this.sysUserMapper = sysUserMapper; this.profiles = profiles;
    }

    @Override
    public CurrentUserVO getMe() {
        SysUser u = currentUser();
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRole(u.getRole());
        vo.setEmailVerified(u.getEmailVerified());
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
        vo.setEmailVerified(u.getEmailVerified());
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

    @org.springframework.transaction.annotation.Transactional
    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        SysUser u = currentUser();
        u = sysUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>().eq(SysUser::getId,u.getId()).last("FOR UPDATE"));
        String previousSchool = u.getSchool();
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
        if (UserRole.USER.name().equals(u.getRole()) && !java.util.Objects.equals(previousSchool, u.getSchool())) {
            com.lanrecruitment.domain.entity.StudentProfile p=profiles.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.lanrecruitment.domain.entity.StudentProfile>().eq(com.lanrecruitment.domain.entity.StudentProfile::getUserId,u.getId()));
            if (p==null) { p=new com.lanrecruitment.domain.entity.StudentProfile(); p.setUserId(u.getId()); p.setProfileVersion(1L); profiles.insert(p); }
            else { p.setProfileVersion(p.getProfileVersion()+1); profiles.updateById(p); }
        }
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

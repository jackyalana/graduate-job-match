package com.lanrecruitment.config;

import cn.dev33.satoken.stp.StpInterface;
import com.lanrecruitment.common.enums.AuditStatus;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.UserRole;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.mapper.SysUserMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SaTokenStpInterfaceImpl implements StpInterface {

    private final SysUserMapper sysUserMapper;

    public SaTokenStpInterfaceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId;
        try {
            userId = Long.valueOf(String.valueOf(loginId));
        } catch (Exception e) {
            return Collections.emptyList();
        }

        SysUser u = sysUserMapper.selectById(userId);
        if (u == null) {
            return Collections.emptyList();
        }
        if (u.getStatus() == null || u.getStatus() != CommonStatus.ENABLED.getCode()) {
            return Collections.emptyList();
        }
        UserRole role = UserRole.from(u.getRole());
        if (role == null) {
            return Collections.emptyList();
        }
        if (role == UserRole.HR && (u.getAuditStatus() == null || u.getAuditStatus() != AuditStatus.PASS.getCode())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(role.name());
    }
}

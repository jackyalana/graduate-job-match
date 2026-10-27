package com.lanrecruitment.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.lanrecruitment.common.enums.UserRole;
import com.lanrecruitment.domain.entity.SysUser;
import com.lanrecruitment.mapper.SysUserMapper;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenConfig {
    @Bean
    public SaServletFilter saServletFilter(SysUserMapper users) {
        return new SaServletFilter().addInclude("/api/**").addExclude("/api/health", "/api/public/**", "/api/jobs", "/api/jobs/*")
            .setAuth(obj -> {
                if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) return;
                StpUtil.checkLogin();
                SysUser user = users.selectById(StpUtil.getLoginIdAsLong());
                if (user == null || !Integer.valueOf(1).equals(user.getStatus()) ||
                    (UserRole.HR.name().equals(user.getRole()) && !Integer.valueOf(1).equals(user.getAuditStatus()))) {
                    StpUtil.logout();
                    throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.BE_REPLACED, "账号不可用", null);
                }
                SaRouter.match("/api/admin/**", r -> StpUtil.checkRole(UserRole.ADMIN.name()));
                SaRouter.match("/api/hr/**", r -> StpUtil.checkRole(UserRole.HR.name()));
                SaRouter.match("/api/user/**", r -> StpUtil.checkRole(UserRole.USER.name()));
                SaRouter.match("/api/student/**", r -> StpUtil.checkRole(UserRole.USER.name()));
            }).setError(e -> {
                int status = e instanceof NotLoginException ? 401 : e instanceof NotRoleException ? 403 : 500;
                String code = status == 401 ? "AUTH_NOT_LOGGED_IN" : status == 403 ? "FORBIDDEN" : "SYSTEM_ERROR";
                String message = status == 401 ? "未登录或账号不可用" : status == 403 ? "无权限" : "系统异常";
                SaHolder.getResponse().setStatus(status).setHeader("Content-Type", "application/json;charset=UTF-8");
                return "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"data\":null,\"details\":null,\"requestId\":\"" + MDC.get("requestId") + "\"}";
            });
    }
}

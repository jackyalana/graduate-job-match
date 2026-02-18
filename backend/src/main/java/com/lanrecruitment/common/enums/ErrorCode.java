package com.lanrecruitment.common.enums;

public enum ErrorCode {
    VALIDATION_FAILED(400, "参数校验失败"),
    SYSTEM_ERROR(500, "系统异常"),

    AUTH_INVALID_CREDENTIALS(400, "用户名或密码错误"),
    AUTH_ACCOUNT_NOT_FOUND(400, "账号不存在"),
    AUTH_NOT_LOGGED_IN(401, "未登录"),
    AUTH_ACCOUNT_DISABLED(403, "账号已被禁用"),
    AUTH_HR_PENDING_AUDIT(403, "HR账号待审核"),
    AUTH_ROLE_INVALID(400, "角色只能为USER或HR"),
    AUTH_USERNAME_EXISTS(400, "用户名已存在"),
    AUTH_EMAIL_EXISTS(400, "邮箱已存在"),

    USER_OLD_PASSWORD_WRONG(400, "旧密码错误"),

    HR_COMPANY_NAME_REQUIRED(400, "请先在个人中心完善公司名称"),
    HR_ACCOUNT_NOT_FOUND(404, "HR账号不存在"),

    TAG_ALREADY_EXISTS(400, "标签已存在"),
    TAG_NOT_FOUND(404, "标签不存在"),

    NOTICE_NOT_FOUND(404, "公告不存在"),

    JOB_NOT_FOUND(404, "岗位不存在"),
    JOB_NOT_APPLICABLE(400, "岗位不可投递"),

    RESUME_NOT_FOUND(404, "简历不存在"),
    RESUME_NOT_AVAILABLE(400, "简历不可用"),

    APPLY_DUPLICATE(400, "请勿重复投递"),
    APPLY_RECORD_NOT_FOUND(404, "投递记录不存在"),
    APPLY_STATUS_INVALID(400, "状态不合法"),
    APPLY_STATUS_VIEWED_UNSUPPORTED(400, "不支持已查看状态"),
    APPLY_CANNOT_CHANGE_RESUME(400, "当前状态不可更换简历"),

    EMAIL_TOO_FREQUENT(429, "操作频繁，请%s秒后再试"),
    EMAIL_CODE_SEND_FAILED(500, "验证码发送失败"),
    EMAIL_CODE_INVALID(400, "验证码无效"),
    EMAIL_CODE_EXPIRED(400, "验证码已过期"),
    EMAIL_CODE_WRONG(400, "验证码错误"),
    EMAIL_SERVICE_NOT_CONFIGURED(500, "邮件服务未配置"),
    EMAIL_SEND_FAILED(500, "邮件发送失败"),

    FILE_EMPTY(400, "文件不能为空"),
    FILE_TOO_LARGE(400, "文件过大"),
    FILE_TYPE_NOT_SUPPORTED(400, "文件类型不支持"),
    FILE_SAVE_FAILED(500, "文件保存失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

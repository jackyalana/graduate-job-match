package com.lanrecruitment.domain.dto;

import com.lanrecruitment.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.Email;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @Pattern(regexp = UserRole.REGEX_REGISTER, message = "角色只能为USER或HR")
    private String role;

    private String companyName;

    @AssertTrue(message = "HR注册必须填写公司名称")
    public boolean isCompanyNameValid() {
        if (role == null || role.trim().isEmpty()) {
            return true;
        }
        if (!UserRole.HR.name().equalsIgnoreCase(role.trim())) {
            return true;
        }
        return companyName != null && !companyName.trim().isEmpty();
    }
}

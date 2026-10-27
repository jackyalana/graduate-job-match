package com.lanrecruitment.domain.vo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Boolean emailVerified;
    private String companyName;
    private String avatar;
    private Integer status;
    private Integer auditStatus;
    private String realName;
    private String phone;
    private String school;
    private LocalDate graduateDate;
}

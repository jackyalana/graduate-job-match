package com.lanrecruitment.domain.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String companyName;
    private String avatar;
    private Integer status;
    private Integer auditStatus;
    private String realName;
    private String phone;
    private String school;
    private LocalDate graduateDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

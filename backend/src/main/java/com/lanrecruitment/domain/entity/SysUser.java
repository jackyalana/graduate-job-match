package com.lanrecruitment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("sys_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    private String role;

    @TableField("company_name")
    private String companyName;

    private String avatar;

    private Integer status;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("real_name")
    private String realName;

    private String phone;

    private String school;

    @TableField("graduate_date")
    private LocalDate graduateDate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

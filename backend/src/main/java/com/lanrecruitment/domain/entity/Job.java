package com.lanrecruitment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("hr_id")
    private Long hrId;

    @TableField("job_name")
    private String jobName;

    @TableField("company_name")
    private String companyName;

    private String city;

    @TableField("salary_range")
    private String salaryRange;

    @TableField("job_type")
    private String jobType;

    private String description;

    private Long categoryId;
    private String cityCode;
    private Integer salaryMin;
    private Integer salaryMax;
    private Boolean salaryNegotiable;
    private Integer educationMinRank;
    private Integer graduateYearMin;
    private Integer graduateYearMax;
    private Boolean acceptFreshGraduate;
    private java.time.LocalDateTime deadline;
    private Long version;
    private Integer status;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

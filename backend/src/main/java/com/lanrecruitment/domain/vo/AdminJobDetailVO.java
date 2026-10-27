package com.lanrecruitment.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminJobDetailVO {
    private Long id;
    private Long hrId;
    private String hrUsername;
    private String jobName;
    private String companyName;
    private String city;
    private String salaryRange;
    private String jobType;
    private String description;
    private Long version;
    private Long categoryId;
    private String cityCode;
    private Integer salaryMin;
    private Integer salaryMax;
    private Boolean salaryNegotiable;
    private Integer educationMinRank;
    private Integer graduateYearMin;
    private Integer graduateYearMax;
    private Boolean acceptFreshGraduate;
    private LocalDateTime deadline;
    private java.util.List<JobTagVO> tags;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

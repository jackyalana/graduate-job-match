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
public class HrJobVO {
    private Long id;
    private String jobName;
    private String companyName;
    private String city;
    private String salaryRange;
    private String jobType;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime createdAt;
    private Integer applyCount;
}

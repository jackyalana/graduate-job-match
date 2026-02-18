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
public class AdminJobAuditVO {
    private Long id;
    private Long hrId;
    private String jobName;
    private String city;
    private String salaryRange;
    private String jobType;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime createdAt;
}

package com.lanrecruitment.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCardVO {
    private Long id;
    private String jobName;
    private String companyName;
    private String city;
    private String salaryRange;
    private String jobType;
    private String description;
    private BigDecimal matchScore;
    private LocalDateTime createdAt;
}

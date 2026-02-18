package com.lanrecruitment.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrJobApplicationVO {
    private Long applyId;
    private Integer applyStatus;
    private LocalDateTime applyTime;
    private ResumeVO resume;
    private List<ResumeTagVO> resumeTags;
    private BigDecimal matchScore;
}

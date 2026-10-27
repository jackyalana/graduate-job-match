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
public class UserApplyVO {
    private Long applyId;
    private Integer applyStatus;
    private Long version;
    private Long jobId;
    private Boolean snapshotAvailable;
    private Boolean hasAttachment;
    private LocalDateTime applyTime;
    private JobCardVO job;
    private ResumeVO resume;
}

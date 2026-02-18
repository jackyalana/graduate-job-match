package com.lanrecruitment.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyJobDTO {
    @NotNull(message = "jobId不能为空")
    private Long jobId;

    @NotNull(message = "resumeId不能为空")
    private Long resumeId;
}

package com.lanrecruitment.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTagSaveDTO {
    @NotNull(message = "jobId不能为空")
    private Long jobId;

    @Valid
    private List<JobTagItemDTO> items;
}

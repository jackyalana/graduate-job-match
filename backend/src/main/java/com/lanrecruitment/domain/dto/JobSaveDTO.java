package com.lanrecruitment.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSaveDTO {
    private Long id;

    @NotBlank(message = "岗位名称不能为空")
    private String jobName;

    private String city;

    private String salaryRange;

    private String jobType;

    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

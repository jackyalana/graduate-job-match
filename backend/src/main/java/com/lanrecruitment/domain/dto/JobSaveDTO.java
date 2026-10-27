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

    @javax.validation.constraints.Positive private Long categoryId;
    @javax.validation.constraints.Size(max=64) private String cityCode;
    @javax.validation.constraints.Min(0) private Integer salaryMin;
    @javax.validation.constraints.Min(0) private Integer salaryMax;
    private Boolean salaryNegotiable;
    @javax.validation.constraints.Min(1) @javax.validation.constraints.Max(4) private Integer educationMinRank;
    @javax.validation.constraints.Min(1900) @javax.validation.constraints.Max(2100) private Integer graduateYearMin;
    @javax.validation.constraints.Min(1900) @javax.validation.constraints.Max(2100) private Integer graduateYearMax;
    private Boolean acceptFreshGraduate;
    private java.time.LocalDateTime deadline;
    private Long version;
    @NotNull(message = "状态不能为空") private Integer status;

    @javax.validation.constraints.AssertTrue(message="岗位条件不合法")
    public boolean isConditionsValid() {
        return (salaryMin==null || salaryMax==null || salaryMin<=salaryMax)
            && (graduateYearMin==null || graduateYearMax==null || graduateYearMin<=graduateYearMax)
            && (!Boolean.TRUE.equals(salaryNegotiable) || (salaryMin==null && salaryMax==null))
            && (status==null || status==0 || status==1);
    }
}

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
public class ResumeSaveDTO {
    private Long id;

    @NotBlank(message = "简历名称不能为空")
    private String resumeName;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String gender;

    private String city;

    private String education;

    private String workYears;

    private String expectJobType;

    private String expectSalary;

    private String workDesc;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

package com.lanrecruitment.domain.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDTO {
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    private String companyName;

    private String school;

    private LocalDate graduateDate;
}

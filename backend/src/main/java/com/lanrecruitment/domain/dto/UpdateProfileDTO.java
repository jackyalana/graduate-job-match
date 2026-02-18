package com.lanrecruitment.domain.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDTO {
    private String realName;

    private String phone;

    private String companyName;

    private String school;

    private LocalDate graduateDate;
}

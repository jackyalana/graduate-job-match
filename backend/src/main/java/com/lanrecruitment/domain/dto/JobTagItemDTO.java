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
public class JobTagItemDTO {
    @NotNull(message = "tagId不能为空")
    private Long tagId;

    private Integer proficiency;
}

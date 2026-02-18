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
public class ApplyStatusUpdateDTO {
    @NotNull(message = "applyId不能为空")
    private Long applyId;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

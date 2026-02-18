package com.lanrecruitment.domain.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatusUpdateDTO {
    @NotNull(message = "id不能为空")
    private Long id;

    @NotNull(message = "status不能为空")
    @Min(value = 0, message = "status不合法")
    @Max(value = 1, message = "status不合法")
    private Integer status;
}

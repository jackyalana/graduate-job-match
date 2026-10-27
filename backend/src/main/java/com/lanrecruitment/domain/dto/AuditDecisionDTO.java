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
public class AuditDecisionDTO {
    @NotNull(message = "id不能为空")
    private Long id;

    @NotNull(message = "pass不能为空")
    private Boolean pass;

    private Long version;
}
